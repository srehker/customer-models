package org.powertac.contractcustomer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.ContractMarket;
import org.powertac.common.interfaces.ContractNegotiationMessageListener;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.ContractNegotiationMessage;
import org.powertac.common.repo.ContractRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeSeriesRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContractCustomerService extends TimeslotPhaseProcessor implements
		ContractNegotiationMessageListener, InitializationService,
		CustomerServiceAccessor {

	static private Logger log = Logger.getLogger(ContractCustomerService.class
			.getName());

	@Autowired
	private ContractMarket contractMarketService;

	@Autowired
	private CustomerRepo customerRepo;

	@Autowired
	private ServerConfiguration serverPropertiesService;

	@Autowired
	private RandomSeedRepo randomSeedRepo;

	@Autowired
	private TimeslotRepo timeslotRepo;

	@Autowired
	private WeatherReportRepo weatherReportRepo;

	@Autowired
	private TariffRepo tariffRepo;

	@Autowired
	private ContractRepo contractRepo;

	@Autowired
	private TimeSeriesRepo timeSeriesRepo;

	@Autowired
	private TariffSubscriptionRepo tariffSubscriptionRepo;

	/** Random Number Generator */
	// private RandomSeed rs1;

	int seedId = 1;

	// read this from configurator
	private String configFile1 = null;
	// private int daysOfCompetition = 0;

	/**
	 * This is the configuration file that will be utilized to pass the
	 * parameters that can be adjusted by user
	 */
	Properties configuration = new Properties();

	/** List of the Customers in the competition */
	ArrayList<ContractCustomer> contractCustomerList;

	/** This is the constructor of the Office Consumer Service. */
	public ContractCustomerService() {
		super();
		contractCustomerList = new ArrayList<ContractCustomer>();
	}

	/**
	 * This function called once at the beginning of each game by the server
	 * initialization service. Here is where you do pre-game setup.
	 */
	@Override
	public String initialize(Competition competition,
			List<String> completedInits) {
		if (!completedInits.contains("DefaultBroker")
				|| !completedInits.contains("TariffMarket"))
			return null;
		super.init();
		contractMarketService.registerContractNegotiationMessageListener(this);

		serverPropertiesService.configureMe(this);

		contractCustomerList.clear();

		ContractCustomer c1 = new ContractCustomer(PowerType.CONSUMPTION);
		c1.setServiceAccessor(this);
		contractCustomerList.add(c1);
		c1.initialize();
		customerRepo.add(c1.getCustomerInfo(PowerType.CONSUMPTION));

		ContractCustomer p1 = new ContractCustomer(PowerType.PRODUCTION);
		p1.setServiceAccessor(this);
		contractCustomerList.add(p1);
		p1.initialize();
		customerRepo.add(p1.getCustomerInfo(PowerType.PRODUCTION));

		return "ContractCustomer";
	}

	/**
	 * This function finds all the available Contract Customers in the
	 * competition and creates a list of their customerInfo.
	 * 
	 * @return List<CustomerInfo>
	 */
	public List<CustomerInfo> generateCustomerInfoList() {
		ArrayList<CustomerInfo> result = new ArrayList<CustomerInfo>();
		for (ContractCustomer contractCustomer : contractCustomerList) {
			for (CustomerInfo customer : contractCustomer.getCustomerInfos())
				result.add(customer);
		}
		return result;
	}

	@Override
	public void activate(Instant time, int phaseNumber) {
		log.info("Activate");
		if (contractCustomerList.size() > 0) {
			for (ContractCustomer contractCustomer : contractCustomerList) {
				contractCustomer.step();
			}
		}
	}

	@Override
	public void setDefaults() {
	}

	// ============== CustomerServiceAccessor API
	@Override
	public CustomerRepo getCustomerRepo() {
		return customerRepo;
	}

	@Override
	public RandomSeedRepo getRandomSeedRepo() {
		return randomSeedRepo;
	}

	@Override
	public TariffRepo getTariffRepo() {
		return tariffRepo;
	}

	@Override
	public TariffSubscriptionRepo getTariffSubscriptionRepo() {
		return tariffSubscriptionRepo;
	}

	@Override
	public TimeslotRepo getTimeslotRepo() {
		return timeslotRepo;
	}

	@Override
	public WeatherReportRepo getWeatherReportRepo() {
		return weatherReportRepo;
	}

	@Override
	public ServerConfiguration getServerConfiguration() {
		return null;
	}

	@Override
	public ContractRepo getContractRepo() {
		return contractRepo;
	}

	@Override
	public TimeSeriesRepo getTimeSeriesRepo() {
		return timeSeriesRepo;
	}

	@Override
	public void onMessage(ContractNegotiationMessage msg) {
		// TODO notfalls hier nachricht weiterreichen

	}

}
