/*
 * Copyright (c) 2014, 2015 by John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.customer.coldstorage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.customer.AbstractCustomer;

/**
 * Model of a cold-storage warehouse with multiple refrigeration units.
 * The size of the refrigeration units is specified as stockCapacity. The
 * number is indeterminate - as many as needed will be used, depending on
 * heat loss and current internal temperature. If currentTemp < nominalTemp and
 * falling or steady, then a unit will be de-energized. If currentTemp >=
 * nominalTemp and rising or steady, then another unit will be activated. 
 * 
 * @author John Collins
 */
@Domain
@ConfigurableInstance
public class ColdStorage
extends AbstractCustomer
implements CustomerModelAccessor
{
  static private Logger log = Logger.getLogger(ColdStorage.class.getName());

  // handy contstants
  static final double R_CONVERSION = 3.1545 / 1000.0; // kW/m^2-K
  static final double TON_CONVERSION = 3.504; // kW heat
  static final double CP_ICE = 0.564; // kWh/tonne-K
  static final double GROUND_TEMP = 3.0; // don't freeze the ground

  // model parameters
  private double minTemp = -35.0; // deg C
  private double maxTemp = -10.0;
  private double nominalTemp = -20.0;
  private double shiftSag = 4.0; // temp diff allowed for cost-shift
  private double evalEnvTemp = 20.0; // assumed outside temp for tariff eval

  private double roofArea = 900.0; //m^2
  private double roofRValue = 40.0;
  private double wallArea = 1440.0; //m^2
  private double wallRValue = 22.0;
  private double floorRValue = 15.0; // area same as roof
  private double infiltrationRatio = 0.5; // added to (wall + roof) loss

  private double cop = 1.5; // coefficient of performance
  private double stockCapacity = 500.0; // tonnes of water ice
  private double turnoverRatio = 0.1; // new stock/day
  private double turnoverSd = 0.015; // sd of turnover
  private double newStockTemp = -5.0; // temperature of incoming stock
  private double nonCoolingUsage = 15.0; // kW nominal
  private double ncUsageVariability = 0.2; // for m-r random walk
  private double ncMeanReversion = 0.06;
  private double unitSize = 40.0; // tons
  private double hysteresis = 0.04; // control range

  // model state
  private PowerType powerType;
  private RandomSeed opSeed;
  private NormalDistribution normal01;
  private RandomSeed evalSeed;

  private double totalEnergyUsed = 0.0;
  private double currentNcUsage;
  private double coolingLossPerK = 0.0; // kWh/K -- lazy computation

  // bootstrap state elements
  @ConfigurableValue(valueType = "Double",
      bootstrapState = true,
      description = "current temperature")
  private Double currentTemp = null;

  @ConfigurableValue(valueType = "Double",
      bootstrapState = true,
      description = "current thermal mass")
  private double currentStock = 0.0;

  private TariffEvaluator tariffEvaluator;
  private int profileSize = 168; // 1 week to accomodate weekly TOU

  /**
   * Default constructor, requires manual setting of name
   */
  public ColdStorage ()
  {
    super();
  }

  /**
   * Constructor with name
   */
  public ColdStorage (String name)
  {
    super(name);
  }

  @Override
  public void initialize ()
  {
    super.initialize();
    log.info("Initialize " + name);
    // fill out CustomerInfo
    powerType = PowerType.THERMAL_STORAGE_CONSUMPTION;
    CustomerInfo info = new CustomerInfo(name, 1);
    info.withPowerType(powerType)
        .withControllableKW(-unitSize / cop)
        .withStorageCapacity(stockCapacity * CP_ICE * (maxTemp - minTemp))
        .withUpRegulationKW(-unitSize / cop)
        .withDownRegulationKW(unitSize / cop); // optimistic, perhaps
    addCustomerInfo(info);
    ensureSeeds();
    // randomize current temp only if state not set
    if (null == currentTemp) {
      setCurrentTemp(minTemp + (maxTemp - minTemp) * opSeed.nextDouble());
      currentStock = stockCapacity;
    }
    currentNcUsage = nonCoolingUsage;
    // set up the tariff evaluator. We are wide-open to variable pricing.
    tariffEvaluator = new TariffEvaluator(this);
    tariffEvaluator.withInertia(0.7).withPreferredContractDuration(14);
    tariffEvaluator.initializeInconvenienceFactors(0.0, 0.01, 0.0, 0.0);
    tariffEvaluator.initializeRegulationFactors(-getMaxCooling() * 0.05,
                                                0.0,
                                                getMaxCooling() * 0.04);
  }

  // Gets a new random-number opSeed just in case we don't already have one.
  // Useful for mock-based testing.
  private void ensureSeeds ()
  {
    if (null == opSeed) {
      opSeed = service.getRandomSeedRepo()
          .getRandomSeed(ColdStorage.class.getName() + "-" + name,
                         0, "model");
      evalSeed = service.getRandomSeedRepo()
          .getRandomSeed(ColdStorage.class.getName() + "-" + name,
                         0, "eval");
      normal01 = new NormalDistribution(0.0, 1.0);
      normal01.reseedRandomGenerator(opSeed.nextLong());
    }
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    return getCustomerInfo(powerType);
  }

  // ----------------------- Run the model ------------------------
  @Override
  public void step ()
  {
    totalEnergyUsed = 0.0;

    // First, we have to account for controls exercised in the last timeslot.
    // If there was non-zero regulation, we have to adjust the temperature.
    double regulation = getSubscription().getRegulation();
    if (regulation != 0.0) {
      // positive value is up-regulation, which means we lost that much
      double tempChange = regulation * cop / currentStock / CP_ICE;
      log.info(getName() + ": regulation = " + regulation
               + ", tempChange = " + tempChange);
      setCurrentTemp(currentTemp + tempChange);
    }

    // add in temp change due to stock turnover
    setCurrentTemp(currentTemp + turnoverRise());

    // start with the non-cooling load - this part is not subject to regulation
    updateNcUsage();
    useEnergy(currentNcUsage);

    // use cooling energy to maintain and adjust current temp
    WeatherReport weather = 
        service.getWeatherReportRepo().currentWeatherReport();
    double outsideTemp = weather.getTemperature();
    double priceAdjustment = computePriceAdjustment(getTariffInfo());
    EnergyInfo info =
        computeCoolingEnergy(getCurrentTemp(),
                             getNominalTemp(),
                             outsideTemp);
    setCurrentTemp(currentTemp + info.getDeltaTemp());

    // Now we need to record available regulation capacity. Note that only
    // the cooling portion is available for regulation.
    // Note also that we have to stay within the min-max temp range
    double availableUp = info.getEnergy() / cop;
    if (currentTemp >= maxTemp)
      // can't regulate up above max temp
      availableUp = 0.0;
    double availableDown = -(getMaxCooling() - info.getEnergy()) / cop;
    if (currentTemp <= minTemp)
      // and can't regulate down below min
      availableDown = 0.0;
    RegulationCapacity capacity =
      new RegulationCapacity(getSubscription(), availableUp, availableDown);
    getSubscription().setRegulationCapacity(capacity);
    log.info(getName()
             + ": regulation capacity (" + capacity.getUpRegulationCapacity()
             + ", " + capacity.getDownRegulationCapacity() + ")");

    useEnergy(info.getEnergy() / cop);

    log.debug("total energy = " + totalEnergyUsed);
    getSubscription().usePower(totalEnergyUsed);
  }

  // digs out the current subscription for this thing. Since the population is
  // always one, there should only ever be one of them
  private TariffSubscription getSubscription ()
  {
    List<TariffSubscription> subs = getCurrentSubscriptions(powerType);
    if (subs.size() > 1) {
      log.warn("Multiple subscriptions " + subs.size() + " for " + getName());
    }
    return subs.get(0);
  }

  // Returns an adjustment factor in the range [-1 .. +1] 
  // based on energy price. 
  private double computePriceAdjustment (TariffInfo info)
  {
    return 0.0;
  }

  // separated out to help create profiles
  // TODO - handle TOU and variable-rate tariffs
  // TODO - don't change model state here.
  EnergyInfo computeCoolingEnergy (double currentTemp,
                                   double targetTemp,
                                   double outsideTemp)
  {
    EnergyInfo result = new EnergyInfo();
    double coolingLoss = computeCoolingLoss(outsideTemp);
    // at this point, coolingLoss is the energy needed to maintain current temp
    double adjustmentCooling = 0.0;
    if (currentTemp < (targetTemp - hysteresis / 2.0)) {
      // go to nominal as quickly as possible
      double maxWarming = coolingLoss;
      double neededWarming =
          currentStock * CP_ICE * (targetTemp - currentTemp);
      adjustmentCooling = -Math.min(maxWarming, neededWarming);
    }
    else if (currentTemp > (targetTemp + hysteresis / 2.0)) {
      double maxCooling = getMaxCooling() - coolingLoss;
      double neededCooling =
          currentStock * CP_ICE * (currentTemp - targetTemp);
      adjustmentCooling = Math.min(neededCooling, maxCooling);
    }
    result.setDeltaTemp(-adjustmentCooling / (currentStock * CP_ICE));
    result.setEnergy(coolingLoss + adjustmentCooling);
    log.info(getName() + ": temp = " + currentTemp
             + ", adjustmentCooling = " + adjustmentCooling
             + ", total cooling energy = " + result.getEnergy()
             + ", temp change = "
             + (-adjustmentCooling / (currentStock * CP_ICE)));
    return result;
  }

  // computes rise in temperature due to stock turnover
  double turnoverRise ()
  {
    double turnoverMean = turnoverRatio * stockCapacity / 24.0;
    double sd = turnoverSd * stockCapacity / 24.0;
    // draw turnover quantity this hour from normal distribution
    double outgoing =
        Math.max(0.0, (normal01.sample() * sd + turnoverMean));
    double incoming =
        Math.max(0.0, (normal01.sample() * sd + turnoverMean));
    currentStock -=  outgoing;
    double newStock = incoming; // daily-hourly
    double newTemp =
      ((currentStock * currentTemp + newStock * newStockTemp)
          / (currentStock + newStock));
    log.info(getName() + ": remove " + outgoing + "T, add " + incoming
             + "T raises temp " + (newTemp - currentTemp) + "K");
    currentStock += incoming;
    return (newTemp - currentTemp);
  }

  void updateNcUsage() // pkg visibility for testing
  {
    if (ncUsageVariability == 0)
      return;
    currentNcUsage = currentNcUsage
        + (nonCoolingUsage
            * (ncUsageVariability * (opSeed.nextDouble() * 2.0 - 1.0)))
            + ncMeanReversion * (nonCoolingUsage - currentNcUsage);
    currentNcUsage = Math.max(0.0, currentNcUsage);
    log.info(getName() + ": Non-cooling usage = " + currentNcUsage);
  }

  // computes kWh cooling energy to maintain current inside temp
  double computeCoolingLoss (double outsideTemp)
  {
    double upperLoss = getCoolingLossPerK() * (outsideTemp - currentTemp);
    double floorLoss =
      (R_CONVERSION / getFloorRValue() * getRoofArea())
          * (GROUND_TEMP - currentTemp);
    log.info(getName() + ": heat loss walls & roof: " + upperLoss
             + ", floor: " + floorLoss
             + ", heat load: " + currentNcUsage);
    return upperLoss + floorLoss + currentNcUsage;
  }

  // Lazy evaluation for walls + roof + infiltration loss rate kW per K
  double getCoolingLossPerK ()
  {
    if (0.0 == coolingLossPerK) {
      double roofLoss = R_CONVERSION / getRoofRValue() * getRoofArea();
      double wallLoss = R_CONVERSION / getWallRValue() * getWallArea();
      double infiltrationLoss = getInfiltrationRatio() * (roofLoss + wallLoss);
      log.debug(": Heat loss per K -- roof: " + roofLoss
                + ", walls: " + wallLoss
                + ", infiltration: " + infiltrationLoss);
      coolingLossPerK = roofLoss + wallLoss + infiltrationLoss;
    }
    return coolingLossPerK;
  }

  // -------------------------- Evaluate tariffs ------------------------
  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    log.info(getName() + ": evaluate tariffs");
    tariffEvaluator.evaluateTariffs();
  }

  // ------------- CustomerModelAccessor methods -----------------
  // TODO - make this tariff-dependent
  private Map<Tariff, TariffInfo> profiles = null;
  double nominalHourlyConsumption = 0.0;
  @Override
  public double[] getCapacityProfileStartingNextTimeSlot (Tariff tariff)
  {
    // lazy creation of profile table
    if (null == profiles) {
      profiles = new HashMap<Tariff, TariffInfo>();
    }
    // return existing profile if it exists
    TariffInfo info = profiles.get(tariff);
    if (null != info) {
      return info.getProfile();
    }
    // otherwise, create a new profile
    info = makeTariffInfo(tariff);
    if (tariff.isTimeOfUse()) {
      heuristicTouProfile(info);
    }
    else {
      // fill profile with nominal consumption
      double[] pr = new double[profileSize];
      Arrays.fill(pr, getNominalHourlyConsumption());
      info.setProfile(pr);
    }
    log.debug(getName() + " profile " + Arrays.toString(info.getProfile()));
    profiles.put(tariff, info);
    return info.getProfile();
  }

  // Should be non-null for any tariff other than the default tariff
  private TariffInfo getTariffInfo (Tariff tariff)
  {
    if (null == profiles)
      return null;
    return profiles.get(tariff);
  }

  // Returns the tariffInfo for the current subscribed tariff
  private TariffInfo getTariffInfo ()
  {
    if (null == profiles)
      return null;
    TariffSubscription sub = getSubscription();
    return profiles.get(sub.getTariff());
  }

  TariffInfo makeTariffInfo (Tariff tariff)
  {
    return new TariffInfo(tariff);
  }

  private double getNominalHourlyConsumption ()
  {
    if (0.0 == nominalHourlyConsumption) {
      double turnoverUsage =
          stockCapacity * (turnoverRatio / 24.0) * CP_ICE
          * (newStockTemp - getNominalTemp()) / cop;
      double maintenanceUsage =
          getCoolingLossPerK() * (getEvalEnvTemp() - getNominalTemp()) / cop;
      nominalHourlyConsumption =
          nonCoolingUsage + turnoverUsage + maintenanceUsage;
      log.info(getName()
               + " turnoverUsage " + turnoverUsage
               + ", maintenanceUsage " + maintenanceUsage
               + ", nominalHourlyConsumption " + nominalHourlyConsumption);
    }
    return nominalHourlyConsumption;
  }

  void heuristicTouProfile (TariffInfo tariffInfo)
  {
    double nhc = getNominalHourlyConsumption();
    // Start with a price profile.
    //log.debug(getName() + " price profile " + Arrays.toString(prices));
    // Characterize the price variability
    double mean = tariffInfo.getMeanPrice();
    double nominalCooling = nhc - nonCoolingUsage;
    double maxCooling = getMaxCooling() / getCop();
    double kwhRange = Math.min(nominalCooling,
                               (maxCooling - nominalCooling));
    double priceRange = Math.max((tariffInfo.getMaxPrice() - mean),
                                 (mean - tariffInfo.getMinPrice()));
    double scaleFactor = kwhRange / priceRange;
    log.debug(getName() + " mean " + mean
              + ", max " + tariffInfo.getMaxPrice()
              + ", min " + tariffInfo.getMinPrice()
              + ", scaleFactor " + scaleFactor);
    //double maxRatio = stats.getMax() / gmean;
    // Generate a profile
    double evalTemp = getNominalTemp();
    double[] result = new double[profileSize];
    EnergyInfo info =
        computeCoolingEnergy(evalTemp + turnoverRise(),
                             getNominalTemp(), getEvalEnvTemp());
    double coolingKwh = info.getEnergy() / getCop();
    log.debug(getName() + " coolingEnergy " + coolingKwh
              + ", max cooling " + maxCooling
              + ", nominal cooling " + nominalCooling);
    double[] prices = tariffInfo.getPrices();
    for (int i = 0; i < profileSize; i++) {
      // stock turnover changes temp
      //evalTemp += turnoverRise();
      double actual =
          nominalCooling + (mean - prices[i]) * scaleFactor;
      result[i] = actual  + nonCoolingUsage;
      //evalTemp += info.getDeltaTemp();
    }
    tariffInfo.setProfile(result);
  }

  // Retrieves an array of 

  @Override
  public double getBrokerSwitchFactor (boolean isSuperseding)
  {
    if (isSuperseding)
      return 0;
    else
      return 0.02;
  }

  @Override
  public double getTariffChoiceSample ()
  {
    return evalSeed.nextDouble();
  }

  @Override
  public double getInertiaSample ()
  {
    return evalSeed.nextDouble();
  }

  // --------------- State and state change -------------
  public double getCurrentTemp ()
  {
    return currentTemp;
  }

  // Returns the maximum kWh that can be expended with the cooling unit
  double getMaxCooling ()
  {
    return unitSize * TON_CONVERSION;
  }

  @StateChange
  void setCurrentTemp (double temp)
  {
    currentTemp = temp;
  }
  
  void useEnergy (double kWh)
  {
    totalEnergyUsed += kWh;
  }

  double getCurrentNcUsage ()
  {
    return currentNcUsage;
  }

  // ----------------- Parameter access -----------------

  public double getMinTemp ()
  {
    return minTemp;
  }

  @ConfigurableValue(valueType = "Double",
      description = "minimum allowable temperature")
  @StateChange
  public ColdStorage withMinTemp (double temp)
  {
    minTemp = temp;
    return this;
  }

  public double getMaxTemp ()
  {
    return maxTemp;
  }

  @ConfigurableValue(valueType = "Double",
      description = "maximum allowable temperature")
  @StateChange
  public ColdStorage withMaxTemp (double temp)
  {
    maxTemp = temp;
    return this;
  }

  public double getNominalTemp ()
  {
    return nominalTemp;
  }

  @ConfigurableValue(valueType = "Double",
      description = "allowable temperature change to save money on TOU tariffs")
  @StateChange
  public void setShiftSag (double deltaT)
  {
    shiftSag = deltaT;
  }

  public double getShiftSag ()
  {
    return shiftSag;
  }

  @ConfigurableValue(valueType = "Double",
      description = "assumed outdoor temp for tariff evaluation")
  @StateChange
  public void setEvalEnvTemp (double temp)
  {
    evalEnvTemp = temp;
  }

  public double getEvalEnvTemp ()
  {
    return evalEnvTemp;
  }

  @ConfigurableValue(valueType = "Double",
      description = "nominal internal temperature")
  @StateChange
  public ColdStorage withNominalTemp (double temp)
  {
    maxTemp = temp;
    return this;
  }

  public double getNewStockTemp ()
  {
    return newStockTemp;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Temperature of incoming stock")
  @StateChange
  public ColdStorage withNewStockTemp (double temp)
  {
    newStockTemp = temp;
    return this;
  }

  public double getStockCapacity ()
  {
    return stockCapacity;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Typical inventory in tonnes of H2O")
  @StateChange
  public ColdStorage withStockCapacity (double value)
  {
    if (value < 0.0)
      log.error(getName() + ": Negative stock capacity " + value
                + " not allowed");
    else
      stockCapacity = value;
    return this;
  }

  public double getTurnoverRatio ()
  {
    return turnoverRatio;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Ratio of stock that gets replaced daily")
  @StateChange
  public ColdStorage withTurnoverRatio (double ratio)
  {
    if (ratio < 0.0 || ratio > 1.0)
      log.error(getName() + ": turnover ratio " + ratio + " out of range");
    else
      turnoverRatio = ratio;
    return this;
  }

  public double getRoofArea ()
  {
    return roofArea;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Area of roof")
  @StateChange
  public ColdStorage withRoofArea (double area)
  {
    roofArea = area;
    return this;
  }

  public double getRoofRValue ()
  {
    return roofRValue;
  }

  @ConfigurableValue(valueType = "Double",
      description = "R-value of roof insulation")
  @StateChange
  public ColdStorage withRoofRValue (double value)
  {
    roofRValue = value;
    return this;
  }

  public double getWallArea ()
  {
    return wallArea;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Total area of outside walls")
  @StateChange
  public ColdStorage withWallArea (double area)
  {
    wallArea = area;
    return this;
  }

  public double getWallRValue ()
  {
    return wallRValue;
  }

  @ConfigurableValue(valueType = "Double",
      description = "R-value of wall insulation")
  @StateChange
  public ColdStorage withWallRValue (double value)
  {
    wallRValue = value;
    return this;
  }

  public double getFloorRValue ()
  {
    return floorRValue;
  }

  @ConfigurableValue(valueType = "Double",
      description = "R-value of floor insulation")
  @StateChange
  public ColdStorage withFloorRValue (double value)
  {
    floorRValue = value;
    return this;
  }

  public double getInfiltrationRatio ()
  {
    return infiltrationRatio;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Infiltration loss as proportion of wall + roof loss")
  @StateChange
  public ColdStorage withInfiltrationRatio (double value)
  {
    if (value < 0.0)
      log.error(getName() + ": Infiltration ratio " + value
                + " cannot be negative");
    else
      infiltrationRatio = value;
    return this;
  }

  public double getUnitSize ()
  {
    return unitSize;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Thermal capacity in tons of cooling plant")
  @StateChange
  public ColdStorage withUnitSize (double cap)
  {
    if (cap < 0.0)
      log.error(getName() + ": Cooling capacity " + cap
                + " cannot be negative");
    else
      unitSize = cap;
    return this;
  }

  public double getCop ()
  {
    return cop;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Coefficient of Performance of refrigeration unit")
  @StateChange
  public ColdStorage withCop (double value)
  {
    if (value < 0.0)
      log.error(getName() + ": Coefficient of performance " + value
                + " cannot be negative");
    else
      cop = value;
    return this;
  }

  public double getHysteresis ()
  {
    return hysteresis;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Control range for refrigeration unit")
  @StateChange
  public ColdStorage withHysteresis (double value)
  {
    if (value < 0.0)
      log.error(getName() + ": Hysteresis " + value
                + " cannot be negative");
    else
      hysteresis = value;
    return this;
  }

  public double getNonCoolingUsage ()
  {
    return nonCoolingUsage;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Mean hourly energy usage for non-cooling purposes")
  @StateChange
  public ColdStorage withNonCoolingUsage (double value)
  {
    if (value < 0.0)
      log.error(getName() + ": Non-cooling usage " + value
                + " cannot be negative");
    else
      nonCoolingUsage = value;
    return this;
  }

  /**
   * Data structure to hold energy and temperature-change info
   * @author jcollins
   */
  class EnergyInfo
  {
    private double energy;
    private double deltaTemp;

    EnergyInfo ()
    {
      super();
    }

    void setEnergy (double kWh)
    {
      energy = kWh;
    }

    double getEnergy ()
    {
      return energy;
    }

    void setDeltaTemp (double dt)
    {
      deltaTemp = dt;
    }

    double getDeltaTemp ()
    {
      return deltaTemp;
    }
  }

  /**
   * Info about evaluated tariffs that might be useful if we subscribe
   */
  class TariffInfo
  {
    private Tariff tariff;
    double[] prices;
    double[] profile;
    private DescriptiveStatistics stats;

    TariffInfo (Tariff tariff)
    {
      super();
      this.tariff = tariff;
    }

    Tariff getTariff ()
    {
      return tariff;
    }

    // true just in case the tariff is a TOU tariff. If it's not, then
    // the price array and profile will be empty (null).
    boolean isTOU ()
    {
      return tariff.isTimeOfUse();
    }

    // prices 00:00 Monday through 23:00 Sunday
    double[] getPrices ()
    {
      if (null != this.prices)
        return prices;
      double nhc = getNominalHourlyConsumption();
      prices = new double[profileSize];
      double cumulativeUsage = 0.0;
      Instant start =
          service.getTimeslotRepo().currentTimeslot().getStartInstant();
      for (int i = 0; i < profileSize; i++) {
        Instant when = start.plus(i * TimeService.HOUR);
        if (when.get(DateTimeFieldType.hourOfDay()) == 0) {
          cumulativeUsage = 0.0;
        }
        prices[i] =
            tariff.getUsageCharge(when, nhc, cumulativeUsage) / nhc;
        cumulativeUsage += nhc;
      }
      return prices;
    }

    void setPrices (double[] prices)
    {
      this.prices = prices;
    }

    // profile used for evaluation
    double[] getProfile ()
    {
      return profile;
    }

    void setProfile (double[] profile)
    {
      this.profile = profile;
    }

    // For a TOU price, returns the mean price, otherwise the fixed
    // or expected-mean price
    double getMeanPrice ()
    {
      ensureStats();
      return stats.getMean();
    }

    double getMaxPrice ()
    {
      ensureStats();
      return stats.getMax();
    }

    double getMinPrice ()
    {
      ensureStats();
      return stats.getMin();
    }

    private void ensureStats ()
    {
      if (null == stats) {
        stats = new DescriptiveStatistics(getPrices());
      }
    }
  }

  @Override
  public double getShiftingInconvenienceFactor(Tariff tariff) {
    // TODO Auto-generated method stub
    return 0;
  }
}
