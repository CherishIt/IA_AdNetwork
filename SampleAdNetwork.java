package ia;

//thist is  aaa

//hangg
//haha

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.AdNetworkKey;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.AdNetworkReportEntry;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportEntry;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.demand.campaign.auction.CampaignAuctionReport;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import tau.tac.adx.report.publisher.AdxPublisherReportEntry;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BankStatus;

/**
 * 
 * @author Mariano Schain
 * Test plug-in
 * 
 */
public class SampleAdNetwork extends Agent {

	private final Logger log = Logger
			.getLogger(SampleAdNetwork.class.getName());

	/*
	 * Basic simulation information. An agent should receive the {@link
	 * StartInfo} at the beginning of the game or during recovery.
	 */
	@SuppressWarnings("unused")
	private StartInfo startInfo;

	/**
	 * Messages received:
	 * 
	 * We keep all the {@link CampaignReport campaign reports} delivered to the
	 * agent. We also keep the initialization messages {@link PublisherCatalog}
	 * and {@link InitialCampaignMessage} and the most recent messages and
	 * reports {@link CampaignOpportunityMessage}, {@link CampaignReport}, and
	 * {@link AdNetworkDailyNotification}.
	 */
	private final Map<Integer, CampaignReport> campaignReports;
	private final Map<Integer, AdNetworkReport> adNetworkReports;
	private PublisherCatalog publisherCatalog;
	private InitialCampaignMessage initialCampaignMessage;
	private AdNetworkDailyNotification adNetworkDailyNotification;
	
	private final Map<Integer, AdNetworkDailyNotification> notifications;
	
    private final Queue<CampaignData> allCampaign;
    
    
	/*
	 * The addresses of server entities to which the agent should send the daily
	 * bids data
	 */
	private String demandAgentAddress;
	private String adxAgentAddress;

	/*
	 * we maintain a list of queries - each characterized by the web site (the
	 * publisher), the device type, the ad type, and the user market segment
	 */
	private AdxQuery[] queries;

	/**
	 * Information regarding the latest campaign opportunity announced
	 */
	private CampaignData pendingCampaign;

	/**
	 * We maintain a collection (mapped by the campaign id) of the campaigns won
	 * by our agent.
	 */
	private Map<Integer, CampaignData> myCampaigns;

	/*
	 * the bidBundle to be sent daily to the AdX
	 */
	private AdxBidBundle bidBundle;

	/*
	 * The current bid level for the user classification service
	 */
	double ucsBid;

	/*
	 * The targeted service level for the user classification service
	 */
	double ucsTargetLevel;

	/*
	 * current day of simulation
	 */
	private int day;
	private String[] publisherNames;
	private CampaignData currCampaign;
        
        private double midPrice;
        private double maxPrice;
	
	private void computeUcsTargetLevel () {
		
	}
	
	

	public SampleAdNetwork() {
		campaignReports = new HashMap<Integer, CampaignReport>();
        allCampaign = new LinkedList<CampaignData>();
        adNetworkReports = new HashMap<Integer, AdNetworkReport>();
        notifications = new HashMap<Integer, AdNetworkDailyNotification>();
	}

	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();

			// log.fine(message.getContent().getClass().toString());

			if (content instanceof InitialCampaignMessage) {
				handleInitialCampaignMessage((InitialCampaignMessage) content);
			} else if (content instanceof CampaignOpportunityMessage) {
				handleICampaignOpportunityMessage((CampaignOpportunityMessage) content);
			} else if (content instanceof CampaignReport) {
				handleCampaignReport((CampaignReport) content);
			} else if (content instanceof AdNetworkDailyNotification) {
				handleAdNetworkDailyNotification((AdNetworkDailyNotification) content);
			} else if (content instanceof AdxPublisherReport) {
				handleAdxPublisherReport((AdxPublisherReport) content);
			} else if (content instanceof SimulationStatus) {
				handleSimulationStatus((SimulationStatus) content);
			} else if (content instanceof PublisherCatalog) {
				handlePublisherCatalog((PublisherCatalog) content);
			} else if (content instanceof AdNetworkReport) {
				handleAdNetworkReport((AdNetworkReport) content);
			} else if (content instanceof StartInfo) {
				handleStartInfo((StartInfo) content);
			} else if (content instanceof BankStatus) {
				handleBankStatus((BankStatus) content);
			} else if(content instanceof CampaignAuctionReport) {
				hadnleCampaignAuctionReport((CampaignAuctionReport) content);
			}
			else {
				System.out.println("[messageReceived] UNKNOWN Message Received: " + content);
			}

		} catch (NullPointerException e) {
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message." + e);
			return;
		}
	}

	private void hadnleCampaignAuctionReport(CampaignAuctionReport content) {
            // System.out.println("[hadnleCampaignAuctionReport] "+day+" :"+ content.toMyString());
		// ingoring
	}

	private void handleBankStatus(BankStatus content) {
		System.out.println("[handleBankStatus] Day " + day + " :" + content.toString());
	}

	/**
	 * Processes the start information.
	 * 
	 * @param startInfo
	 *            the start information.
	 */
	protected void handleStartInfo(StartInfo startInfo) {
		this.startInfo = startInfo;
                // System.out.println("[handleStartInfo] Day " + day + " :" + startInfo.toString());
	}

	/**
	 * Process the reported set of publishers
	 * 
	 * @param publisherCatalog
	 */
	private void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		generateAdxQuerySpace();
		getPublishersNames();
                // System.out.println("[handlePublisherCatalog] Day " + day + " :" + publisherCatalog.toString());
	}

	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	private void handleInitialCampaignMessage(
			InitialCampaignMessage campaignMessage) {
		System.out.println("[handleInitialCampaignMessage] "+campaignMessage.toString());

		day = 0;

		initialCampaignMessage = campaignMessage;
		demandAgentAddress = campaignMessage.getDemandAgentAddress();
		adxAgentAddress = campaignMessage.getAdxAgentAddress();

		CampaignData campaignData = new CampaignData(initialCampaignMessage);
		campaignData.setBudget(initialCampaignMessage.getBudgetMillis()/1000.0);
		currCampaign = campaignData;
		genCampaignQueries(currCampaign);
                
                midPrice = 0.2*campaignData.budget/((campaignData.dayEnd-campaignData.dayStart)*1.0);
                maxPrice = midPrice;
               
                allCampaign.add(campaignData);

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		System.out.println("[handleInitialCampaignMessage] Day " + day + ": Allocated campaign - " + campaignData);
		myCampaigns.put(initialCampaignMessage.getId(), campaignData);
	}

	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	private void handleICampaignOpportunityMessage(
			CampaignOpportunityMessage com) {

		day = com.getDay();

		pendingCampaign = new CampaignData(com);
		System.out.println("[handleICampaignOpportunityMessage] Day " + day + ": Campaign opportunity - " + pendingCampaign);
                
                
                
                allCampaign.add(pendingCampaign);
		/*
		 * The campaign requires com.getReachImps() impressions. The competing
		 * Ad Networks bid for the total campaign Budget (that is, the ad
		 * network that offers the lowest budget gets the campaign allocated).
		 * The advertiser is willing to pay the AdNetwork at most 1$ CPM,
		 * therefore the total number of impressions may be treated as a reserve
		 * (upper bound) price for the auction.
		 */
                showAllCampaign();
		Random random = new Random();
		long cmpimps = com.getReachImps();
		long cmpBidMillis = random.nextInt((int)cmpimps);
		
		System.out.println("[handleICampaignOpportunityMessage] Day " + day + ": Campaign total budget bid (millis): " + cmpBidMillis);

                
                //getDayOtherCampaign(day);
                //getDayMyCampaign(day);
                // System.out.println("    [getReachLevel] Day " + day + " ReachLevel: " +getReachLevel(day));
                // System.out.println("    [getTakeReachLevel] Day " + day + " ReachTakeLevel: " +getTakeReachLevel(day));
                // System.out.println("    [getUCSDemandlevel] Day " + day + " UCSDemandlevel: "+ getUCSDemandlevel(getReachLevel(day), getTakeReachLevel(day)));
		/*
		 * Adjust ucs bid s.t. target level is achieved. Note: The bid for the
		 * user classification service is piggybacked
		 */

//		if (adNetworkDailyNotification != null) {
//			double ucsLevel = adNetworkDailyNotification.getServiceLevel();
//			ucsBid = 0.1 + random.nextDouble()/10.0;			
//			System.out.println("[handleICampaignOpportunityMessage] Day " + day + ": ucs level reported: " + ucsLevel);
//		} else {
//			System.out.println("[handleICampaignOpportunityMessage] Day " + day + ": Initial ucs bid is " + ucsBid);
//		}
                
                
                
                ucsBid = getUCSbid(getUCSDemandlevel(getReachLevel(day+2), getTakeReachLevel(day+2)));
                System.out.println("    [handleICampaignOpportunityMessage] Day " + day + " ucsBid = "+ ucsBid);
                
		/* Note: Campaign bid is in millis */
		AdNetBidMessage bids = new AdNetBidMessage(ucsBid, pendingCampaign.id, (long)(0.15*cmpimps));
		sendMessage(demandAgentAddress, bids);
	}

	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	private void handleAdNetworkDailyNotification(
			AdNetworkDailyNotification notificationMessage) {

		adNetworkDailyNotification = notificationMessage;

		System.out.println("[handleAdNetworkDailyNotification] Day " + day + ": Daily notification for campaign "
				+ adNetworkDailyNotification.getCampaignId());

		String campaignAllocatedTo = " allocated to "
				+ notificationMessage.getWinner();

		if ((pendingCampaign.id == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCostMillis() != 0)) {

			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCostMillis()/1000.0);
			currCampaign = pendingCampaign;
			genCampaignQueries(currCampaign);
			myCampaigns.put(pendingCampaign.id, pendingCampaign);

			campaignAllocatedTo = " WON at cost (Millis)"
					+ notificationMessage.getCostMillis();
		}
		
		notifications.put(day+1, notificationMessage);
		System.out.println("[PutNotification](day:"+(day+1)+") (EffectDay:" + notificationMessage.getEffectiveDay()+")");
		System.out.println("[handleAdNetworkDailyNotification] Day " + day + ": " + campaignAllocatedTo
				+ ". UCS Level set to " + notificationMessage.getServiceLevel()
				+ " at price " + notificationMessage.getPrice()
				+ " Quality Score is: " + notificationMessage.getQualityScore());
                
                setMidPrice(notificationMessage.getPrice(),notificationMessage.getServiceLevel());
//                if(day>58){
//                    AdNetBidMessage bids = new AdNetBidMessage(0.1 + new Random().nextDouble()/10.0, pendingCampaign.id, (long)100);
//                    sendMessage(demandAgentAddress, bids);
//                }
	}

	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	private void handleSimulationStatus(SimulationStatus simulationStatus) {
		System.out.println("[handleSimulationStatus] Day " + day + " : Simulation Status Received");
		sendBidAndAds();
		System.out.println("[handleSimulationStatus] Day " + day + " ended. Starting next day");
		++day;
	}

	/**
	 * 
	 */
	protected void sendBidAndAds() {

		bidBundle = new AdxBidBundle();

		/*
		 * 
		 */

		int dayBiddingFor = day + 1;

		/* A fixed random bid, for all queries of the campaign */
		/*
		 * Note: bidding per 1000 imps (CPM) - no more than average budget
		 * revenue per imp
		 */

		//double maxBid = 10000.0;

		/*
		 * add bid entries w.r.t. each active campaign with remaining contracted
		 * impressions.
		 * 
		 * for now, a single entry per active campaign is added for queries of
		 * matching target segment.
		 */
		
		
		//Bid for all my active campaigns
		Iterator it = myCampaigns.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry)it.next();
			CampaignData campaign = (CampaignData)entry.getValue();
			//active campaign
			if((dayBiddingFor >= campaign.dayStart)
					&& (dayBiddingFor <= campaign.dayEnd)){
				for(AdxQuery query : campaign.campaignQueries) {
					// maxBid not exceed budget per imp
					double maxBid = 10000 * campaign.budget/campaign.reachImps;
					// default coef = 1 (text, pc)
					double coef = 1;
					double impsCoef = 1;
					double ucsCoef = 1;
					double togoCoef = 1;
					double impsRatio = this.getImpsTogoDayRatio(day, campaign);
					if(impsRatio > 1){
						impsCoef = 3;
					} else if(impsRatio > 0.8){
						impsCoef = 2;
					} else if(impsRatio > 0.6){
						impsCoef = 1.5;
					} else if(impsRatio < 0.3) {
						impsCoef = 0.6;
					}
					double ucs = this.getDayUcsLevel(dayBiddingFor);
					ucsCoef = (impsRatio+0.2)/ucs;
					
					double basicBid = maxBid*coef*impsCoef*ucsCoef*togoCoef;
					
					// weight set to impressionToGo ratio.
					// more need, more weight.
					int weight = (int)((impsRatio+0.2)*100);
					
					
					// if is mobile or video, add 0.9 of corresponding coef
					if(query.getDevice() == Device.mobile)
						coef = 1 + (campaign.mobileCoef-1)*0.9;
					if(query.getAdType() == AdType.video)
						coef = 1 + (campaign.videoCoef-1)*0.9;
					
					//is unknown user
					if(query.getMarketSegments().size() ==0){
						String publisher = query.getPublisher();
						double ratio = this.getCampaignPopRatio(campaign);
						bidBundle.addQuery(query, basicBid*ratio, new Ad(null),
								campaign.id, weight);
					}
					// have competition, bid 0.5*max*coef
					// ?? leave weight here for future.
					else if(haveCompetitor(query.getMarketSegments(), dayBiddingFor)) {
						double bid = maxBid*coef;
						
						
						bidBundle.addQuery(query, basicBid, new Ad(null),
								campaign.id, weight);
					} 
					// no competitor
					else {
						if(campaign.impsTogo() > 0)
							basicBid *= 0.1;
						else
							basicBid *= 0.1;
						bidBundle.addQuery(query, basicBid, new Ad(null), campaign.id, weight);
					}
				}
			}
		}
		
		if (bidBundle != null) {
			System.out.println("[sendBidAndAds] Day " + day + ": Sending BidBundle");
			sendMessage(adxAgentAddress, bidBundle);
		}
		
		/*if ((dayBiddingFor >= currCampaign.dayStart)
				&& (dayBiddingFor <= currCampaign.dayEnd)) {

			int entCount = 0;

			for (AdxQuery query : currCampaign.campaignQueries) {
				if (currCampaign.impsTogo() - entCount > 0) {
					/*
					 * among matching entries with the same campaign id, the AdX
					 * randomly chooses an entry according to the designated
					 * weight. by setting a constant weight 1, we create a
					 * uniform probability over active campaigns(irrelevant because we are bidding only on one campaign)
					 *
					if (query.getDevice() == Device.pc) {
						if (query.getAdType() == AdType.text) {
							entCount++;
						} else {
							entCount += currCampaign.videoCoef;
						}
					} else {
						if (query.getAdType() == AdType.text) {
							entCount+=currCampaign.mobileCoef;
						} else {
							entCount += currCampaign.videoCoef + currCampaign.mobileCoef;
						}

					}
					bidBundle.addQuery(query, rbid, new Ad(null),
							currCampaign.id, 1);
				}
			}

			double impressionLimit = currCampaign.impsTogo();
                        
			double budgetLimit = currCampaign.budget;
			bidBundle.setCampaignDailyLimit(currCampaign.id,
					(int) impressionLimit, budgetLimit);

			System.out.println("[sendBidAndAds] Day " + day + ": Updated " + entCount
					+ " Bid Bundle entries for Campaign id " + currCampaign.id);
		}*/

		
	}

	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	private void handleCampaignReport(CampaignReport campaignReport) {
		if(campaignReport.size()>0){
			this.campaignReports.put(day, campaignReport);
		}
		/*
		 * for each campaign, the accumulated statistics from day 1 up to day
		 * n-1 are reported
		 */
		for (CampaignReportKey campaignKey : campaignReport.keys()) {
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(
					campaignKey).getCampaignStats();
			myCampaigns.get(cmpId).setStats(cstats);

			System.out.println("[handleCampaignReport] Day " + day + ": Updating campaign " + cmpId + " stats: "
					+ cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost());
		}
	}

	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	private void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) {
		System.out.println("[handleAdxPublisherReport] Publishers Report: ");
		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) {
			AdxPublisherReportEntry entry = adxPublisherReport
					.getEntry(publisherKey);
			System.out.println("[handleAdxPublisherReport] "+entry.toString());
		}
	}

	/**
	 * 
	 * @param AdNetworkReport
	 */
	private void handleAdNetworkReport(AdNetworkReport adnetReport) {
		this.adNetworkReports.put(day, adnetReport);

		System.out.println("[handleAdNetworkReport] Day " + day + " : AdNetworkReport");
		
		 for (AdNetworkKey adnetKey : adnetReport.keys()) {	  
			 double rnd = Math.random(); if (rnd > 0.95) { AdNetworkReportEntry
			 entry = adnetReport .getAdNetworkReportEntry(adnetKey);
			 System.out.println(adnetKey + " " + entry); } 
		 }
	}

	@Override
	protected void simulationSetup() {

		day = 0;
		bidBundle = new AdxBidBundle();

		/* initial bid between 0.1 and 0.2 */
		ucsBid = 0.2;

		myCampaigns = new HashMap<Integer, CampaignData>();
		log.fine("AdNet " + getName() + " simulationSetup");
                System.out.println("[simulationSetup] Day " + day + " : ----------------------------------------------------");
	}

	@Override
	protected void simulationFinished() {
		campaignReports.clear();
        allCampaign.clear();
        adNetworkReports.clear();
        notifications.clear();
		bidBundle = null;
                System.out.println("[simulationFinished] Day " + day + " : ----------------------------------------------------");
                //System.exit(-1);
	}

	/**
	 * A user visit to a publisher's web-site results in an impression
	 * opportunity (a query) that is characterized by the the publisher, the
	 * market segment the user may belongs to, the device used (mobile or
	 * desktop) and the ad type (text or video).
	 * 
	 * An array of all possible queries is generated here, based on the
	 * publisher names reported at game initialization in the publishers catalog
	 * message
	 */
	private void generateAdxQuerySpace() {
		if (publisherCatalog != null && queries == null) {
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
				String publishersName = publisherCatalogEntry
						.getPublisherName();
				for (MarketSegment userSegment : MarketSegment.values()) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));

				}

				/**
				 * An empty segments set is used to indicate the "UNKNOWN"
				 * segment such queries are matched when the UCS fails to
				 * recover the user's segments.
				 */
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.text));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);
		}
	}
	
	/*genarates an array of the publishers names
	 * */
	private void getPublishersNames() {
		if (null == publisherNames && publisherCatalog != null) {
			ArrayList<String> names = new ArrayList<String>();
			for (PublisherCatalogEntry pce : publisherCatalog) {
				names.add(pce.getPublisherName());
			}

			publisherNames = new String[names.size()];
			names.toArray(publisherNames);
		}
	}
	/*
	 * genarates the campaign queries relevant for the specific campaign, and assign them as the campaigns campaignQueries field 
	 */
	private void genCampaignQueries(CampaignData campaignData) {
		Set<AdxQuery> campaignQueriesSet = new HashSet<AdxQuery>();
		for (String PublisherName : publisherNames) {
			for (Set<MarketSegment> subSegment : campaignData.subTargetSegment){
				campaignQueriesSet.add(new AdxQuery(PublisherName,
					subSegment, Device.mobile, AdType.text));
				campaignQueriesSet.add(new AdxQuery(PublisherName,
					subSegment, Device.mobile, AdType.video));
				campaignQueriesSet.add(new AdxQuery(PublisherName,
					subSegment, Device.pc, AdType.text));
				campaignQueriesSet.add(new AdxQuery(PublisherName,
					subSegment, Device.pc, AdType.video));
				//add unknown segment
				campaignQueriesSet.add(new AdxQuery(PublisherName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.video));
				campaignQueriesSet.add(new AdxQuery(PublisherName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.text));
				campaignQueriesSet.add(new AdxQuery(PublisherName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				campaignQueriesSet.add(new AdxQuery(PublisherName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			
		}

		campaignData.campaignQueries = new AdxQuery[campaignQueriesSet.size()];
		campaignQueriesSet.toArray(campaignData.campaignQueries);
		System.out.println("[genCampaignQueries] !!!!!!!!!!!!!!!!!!!!!!"+Arrays.toString(campaignData.campaignQueries)+"!!!!!!!!!!!!!!!!");
		

	}
        
        private void showAllCampaign(){
            int count = 1;
            for (CampaignData d: allCampaign){
                System.out.println("    [showAllCampaign] "+"("+count+") "+d);
                count++;
            }
        
        }
        
        private Queue<CampaignData> getDayOtherCampaign(int _day){
            Queue<CampaignData> dayOtherCampaign;
            dayOtherCampaign = new LinkedList<CampaignData>();
            
            for (CampaignData d: allCampaign){
                if(d.dayStart <= _day && d.dayEnd >= _day && myCampaigns.get(d.id) == null){
                    dayOtherCampaign.add(d);
                    System.out.println("    [getDayOtherCampaign] (day: " + _day +") " + d);
                }   
            }
            
            return dayOtherCampaign;
        }
        
        private Queue<CampaignData> getDayMyCampaign(int _day){
            Queue<CampaignData> dayMyCampaign;
            dayMyCampaign = new LinkedList<CampaignData>();
            
            for(CampaignData camp : myCampaigns.values()){
                if(camp.dayStart <= _day && camp.dayEnd >= _day){
                    dayMyCampaign.add(camp);
                    System.out.println("    [getDayMyCampaign] (day: " + _day +") " + camp);
                }
            }
            
            return dayMyCampaign;
        }

        private void setMidPrice(double ucsPrice, double ucsLevel){
            if(midPrice == ucsPrice && ucsLevel < 0.48){midPrice = midPrice*2;}
            if(midPrice == ucsPrice && ucsLevel >= 0.48 && ucsLevel < 0.53){midPrice = midPrice*1.1;}
            if(midPrice == ucsPrice && ucsLevel >= 0.53 && ucsLevel < 0.73){midPrice = midPrice;}
            if(midPrice == ucsPrice && ucsLevel >= 0.73 && ucsLevel < 1){midPrice = midPrice*0.9;}
            if(midPrice == ucsPrice && ucsLevel ==1){midPrice = midPrice*0.5;}
            System.out.println("    [setMidPrice] Day: "+ day +" MidPrice = " + midPrice);
         
        }
        
        private double getUCSbid(double ucsDemandLevel){
            double _ucsBid = 0.0;
            System.out.println("        [getUCSbid] Day: "+ day +" ucsDemandLevel = " + ucsDemandLevel);
            if (ucsDemandLevel<0.4){_ucsBid = maxPrice>0.0005?0.0005:maxPrice;}
            if (ucsDemandLevel>=0.4 && ucsDemandLevel<=0.7){_ucsBid = maxPrice>midPrice?midPrice:maxPrice;}
            if (ucsDemandLevel>0.7){_ucsBid = maxPrice;}

            return _ucsBid;
        }
        
        private double getUCSDemandlevel(Map<Set<MarketSegment>,Double> reachLevel, Map<Set<MarketSegment>,Double> takeLevel){
            double ucsPrecidion = 0.0;
            double cs = 0.0;
            for(Set<MarketSegment> s: reachLevel.keySet()){
                if(reachLevel.get(s)>0){
                    ucsPrecidion += (reachLevel.get(s)*MarketSegment.marketSegmentSize(s)/(1-takeLevel.get(s)));
                    cs += MarketSegment.marketSegmentSize(s);
                }
            }
            if(cs == 0) return 0.0;
            return ucsPrecidion/cs;
        }
        
    // Judge whether Others have campaign on a subSegment
        private Map<Set<MarketSegment>,Double> getReachLevel(int _day){
            Queue<CampaignData> dayMyCampaign = getDayMyCampaign(_day);
            Map<Set<MarketSegment>,Double> reachLevel = genLevelMap();
            
            
            for(CampaignData d : dayMyCampaign){
                // System.out.println("        [getReachLevel] d.subTargetSegment = " + d.subTargetSegment );
                for(Set<MarketSegment> s: d.subTargetSegment){
                    if(day>0)
                        reachLevel.put(s, reachLevel.get(s)+d.stats.getTargetedImps()/(1.0*MarketSegment.marketSegmentSize(d.targetSegment)*(d.dayEnd-(day>d.dayStart?day:d.dayStart))));
                    if(day == 0)
                        reachLevel.put(s, reachLevel.get(s)+d.reachImps/(1.0*MarketSegment.marketSegmentSize(d.targetSegment)*(d.dayEnd-(day>d.dayStart?day:d.dayStart))));
                }
            }
            
            return reachLevel;
        }
        
        private Map<Set<MarketSegment>,Double> genLevelMap(){
            Map<Set<MarketSegment>,Double> reachLevel = new HashMap<Set<MarketSegment>,Double>();
            for(int i=18; i<26; i++){
                reachLevel.put(MarketSegment.marketSegments().get(i), 0.0);
            }
            
            return reachLevel;  
        }
        
        private Map<Set<MarketSegment>,Double> getTakeReachLevel(int _day){
            Queue<CampaignData> dayOtherCampaign = getDayOtherCampaign(_day);
            Map<Set<MarketSegment>,Double> takeLevel = genLevelMap();
            double takeLevDou = 0.0;
            for(CampaignData d : dayOtherCampaign){
                // System.out.println("        [getTakeReachLevel] d.subTargetSegment = " + d.subTargetSegment );
                for(Set<MarketSegment> s: d.subTargetSegment){
                    takeLevDou = d.reachImps/((MarketSegment.marketSegmentSize(d.targetSegment))*(d.dayEnd-d.dayStart)*1.0);
                    // System.out.println("        [getTakeReachLevel] takeLevDou = " + takeLevDou );
                    takeLevel.put(s, takeLevel.get(s)>takeLevDou?takeLevel.get(s):takeLevDou);
                }
            }
            
            
            return takeLevel;
        }

    private boolean haveCompetitor(Set<MarketSegment> seg, int day){
    	if(day <= 5) {
    		System.out.println("[haveCompetitor] (day: " + day + ")" + "(segment:" + seg + ") false : First 5 days, cannot judge.");
    		return true;
    	}
    	for (CampaignData d: allCampaign){
    		// active campaign && not my campaign && contain this segment ---> competitor
            if((d.dayStart <= day && d.dayEnd >= day) &&
                (!myCampaigns.containsKey(d.id)) &&
                (d.subTargetSegment.contains(seg))) {
            	System.out.println("[haveCompetitor] (day: " + day +") " + "(segment:" + seg + ") true :" + d);
            	return true;
            }   
        }
    	System.out.println("[haveCompetitor] (day: " + day +") " + "(segment:" + seg + ")" + "false");
    	return false;
    }
    
    //Get how much Campaign I have on a subSegment
    private int numMyCampOnSeg(Set<MarketSegment> seg, int day) {
    	int num = 0;
    	for(CampaignData d : myCampaigns.values()){
    		if((d.dayStart <= day && d.dayEnd >= day) &&
	                (d.subTargetSegment.contains(seg))) {
	            num++;
	        }
    	}
    	System.out.println("[numMyCampaign] (day: " + day +") " + "(segment:" + seg + "):" + num);
        return num;
    }
    
    // Get competitionRatio for a Campaign (weighted average of all subSegs)
    private double competitionRatio(CampaignData camp, int day) {
    	double ratio = 1;
    	double total = MarketSegment.marketSegmentSize(camp.targetSegment);
    	double competeTotal = 0;
    	for(Set<MarketSegment> subseg : camp.subTargetSegment){
    		int subTotal = MarketSegment.marketSegmentSize(subseg);
    		// other competitor -> competitor
    		if(haveCompetitor(subseg, day)){
    			competeTotal += subTotal;
    		} else {
    			int numMy = numMyCampOnSeg(subseg, day);
    			// my other camp -> not fully compete.
    			if(numMy > 0){
    				competeTotal += subTotal - subTotal/numMy;
    			}
    		}
    	}
    	return competeTotal/total;
    }
    
    private double getSegmentPopRatio(Set<MarketSegment> seg){
    	double segPop = MarketSegment.marketSegmentSize(seg);
    	double allPop = MarketSegment.marketSegmentSize(MarketSegment.compundMarketSegment1(MarketSegment.MALE))
    			+ MarketSegment.marketSegmentSize(MarketSegment.compundMarketSegment1(MarketSegment.FEMALE));

    	double ratio = segPop/allPop;
    	System.out.println("[getSegmentPopRatio] (totalPop: " + allPop 
    			+ ") (segPop: " + segPop + ") ( ratio: "+ ratio  );
    	
    	return ratio;
    }
    
    private double getCampaignPopRatio(CampaignData camp){
    	double ratio = 0;
    	for(Set<MarketSegment> subseg : camp.subTargetSegment){
    		ratio += getSegmentPopRatio(subseg);
    	}
    	return ratio;
    }
    
    private double getDayUcsLevel(int day) {
    	if(day==0){
    		return 0.9;
    	}
    	if(notifications.get(day) != null){
    		double ucs = notifications.get(day).getServiceLevel();
    		System.out.println("[getDayUcsLevel][day:" + day +"]" + ucs);
    		return ucs;
    	}
    	double ucs = 0.6;
    	System.out.println("[getDayUcsLevel][day:" + day +"] (not found)" + ucs);
    	return ucs;
    }
    
    private double getDayQuality(int day) {
    	double quality = notifications.get(day).getQualityScore();
    	System.out.println("[getDayQuality][day:" + day +"]" + quality);
    	return quality;
    }
  
    private static Double getPublisherPop(String publisher){
    	Map<String, Double> pubPop =  new HashMap();
    	pubPop.put("yahoo", 16.0);
    	pubPop.put("cnn", 2.2);
    	pubPop.put("nyt", 3.1);
    	pubPop.put("hfngtn", 8.1);
    	pubPop.put("msn", 18.2);
    	pubPop.put("fox", 3.1);
    	pubPop.put("amazon", 12.8);
    	pubPop.put("ebay", 8.5);
    	pubPop.put("wallmart", 3.8);
    	pubPop.put("target", 2.0);
    	pubPop.put("bestbuy", 1.6);
    	pubPop.put("sears", 1.6);
    	pubPop.put("webmd", 2.5);
    	pubPop.put("ehow", 2.5);
    	pubPop.put("ask", 5.0);
    	pubPop.put("tripadvisor", 1.6);
    	pubPop.put("cnet", 1.7);
    	pubPop.put("wheather", 5.8);
    	return pubPop.get(publisher);
    }
    
    private double getPublisherPopRatio(String publisher) {
    	if(null == this.publisherNames){
    		this.getPublishersNames();
    	}
    	double total = 0;
    	for(String pubName : publisherNames){
    		total += getPublisherPop(pubName);
    	}
    	return getPublisherPop(publisher)/total;
    }
    
    private double getImpsTogoDayRatio(int day, CampaignData camp) {
    	CampaignReport report = campaignReports.get(day);
    	double all = MarketSegment.marketSegmentSize(camp.targetSegment);
    	double imps = camp.reachImps;
    	double dayLeft = camp.dayEnd - day + 1;
    	if(camp.dayStart == day+1){
    		dayLeft--;
    	}
    	if(report != null){
    	for(CampaignReportKey key : report.keys()){
    		if(key.getCampaignId() == camp.id){
    			CampaignReportEntry entry = report.getEntry(key);
    			double targetedImps = entry.getCampaignStats().getTargetedImps();
    			System.out.println("[getImpsTogeDayRatio] "+ "(day:" + day + ") (reachImps:"+imps 
    					+ ") (nowImps" + targetedImps + ") (dayLeft: " + dayLeft + ") (ratio: "+ ((imps-targetedImps)/all)/dayLeft);
    			return ((imps-targetedImps)/all)/dayLeft;
    		}
    	}
    	}
    	
    	System.out.println("[getImpsTogeDayRatio] "+ "(day:" + day + ") (reachImps:"+imps 
				+ ") (nowImps: 0, 1st or 2nd DAY"+ ") (dayLeft: " + dayLeft + ") (ratio: "+ (imps/all)/dayLeft);
    	return (imps/all)/dayLeft;
    }

	private class CampaignData {
		/* campaign attributes as set by server */
		Long reachImps;
		long dayStart;
		long dayEnd;
		Set<MarketSegment> targetSegment;
		List<Set<MarketSegment>> subTargetSegment = new ArrayList<Set<MarketSegment>>();
		double videoCoef;
		double mobileCoef;
		int id;
		private AdxQuery[] campaignQueries;//array of queries relvent for the campaign.

		/* campaign info as reported */
		CampaignStats stats;
		double budget;

		public CampaignData(InitialCampaignMessage icm) {
			reachImps = icm.getReachImps();
			dayStart = icm.getDayStart();
			dayEnd = icm.getDayEnd();
			targetSegment = icm.getTargetSegment();
			videoCoef = icm.getVideoCoef();
			mobileCoef = icm.getMobileCoef();
			id = icm.getId();
			
			
			// if seg is 1 or 2, get all 3 subSeg
			this.addSubTargetSeg(targetSegment);
			
			stats = new CampaignStats(0, 0, 0);
			budget = 0.0;
		}
		
		private void addSubTargetSeg(Set<MarketSegment> tar){
			for(int i = 0; i < 26; i ++) {
				Set<MarketSegment> seg = MarketSegment.marketSegments().get(i);
				if(!(tar.size() == seg.size() && tar.containsAll(seg)))
					continue;
				switch(i){
				case 0:
					this.addSubTargetSeg("FEMALE");
					break;
				case 1:
					this.addSubTargetSeg("MALE");
					break;
				case 2:
					this.addSubTargetSeg("YOUNG");
					break;
				case 3:
					this.addSubTargetSeg("OLD");
					break;
				case 4:
					this.addSubTargetSeg("LOW_INCOME");
					break;
				case 5:
					this.addSubTargetSeg("HIGH_INCOME");
					break;
				case 6:
					this.addSubTargetSeg("FEMALE,YOUNG");
					break;
				case 7:
					this.addSubTargetSeg("FEMALE,OLD");
					break;
				case 8:
					this.addSubTargetSeg("MALE,YOUNG");
					break;
				case 9:
					this.addSubTargetSeg("MALE,OLD");
					break;
				case 10:
					this.addSubTargetSeg("FEMALE,LOW_INCOME");
					break;
				case 11:
					this.addSubTargetSeg("FEMALE,HIGH_INCOME");
					break;
				case 12:
					this.addSubTargetSeg("MALE,LOW_INCOME");
					break;
				case 13:
					this.addSubTargetSeg("MALE,HIGH_INCOME");
					break;
				case 14:
					this.addSubTargetSeg("YOUNG,LOW_INCOME");
					break;
				case 15:
					this.addSubTargetSeg("YOUNG,HIGH_INCOME");
					break;
				case 16:
					this.addSubTargetSeg("OLD,LOW_INCOME");
					break;
				case 17:
					this.addSubTargetSeg("OLD,HIGH_INCOME");
					break;
				default:
					this.subTargetSegment.add(targetSegment);
				}
			}
			System.out.println("subTargetSegments::::!!");
			for(Set<MarketSegment> seg : this.subTargetSegment){
				System.out.println(MarketSegment.names(seg));
			}
		}
		
		private void addSubTargetSeg(String segNames){
			for(int i=18; i<26; i++){
				String[] names = segNames.split(",");
				Boolean isSub = true;
				for(String name : names) {
					if(!MarketSegment.names(MarketSegment.marketSegments().get(i)).contains(name)){
						isSub = false;
					}else if(name == "MALE" && MarketSegment.names(MarketSegment.marketSegments().get(i)).contains("FEMALE")){
						isSub = false;
					}
				}
				if(isSub)
					this.subTargetSegment.add(MarketSegment.marketSegments().get(i));
			} 
		}

		public void setBudget(double d) {
			budget = d;
		}

		public CampaignData(CampaignOpportunityMessage com) {
			dayStart = com.getDayStart();
			dayEnd = com.getDayEnd();
			id = com.getId();
			reachImps = com.getReachImps();
			targetSegment = com.getTargetSegment();
			mobileCoef = com.getMobileCoef();
			videoCoef = com.getVideoCoef();
			stats = new CampaignStats(0, 0, 0);
			budget = 0.0;
			
			// if seg is 1 or 2, get all 3 subSeg
			this.addSubTargetSeg(targetSegment);
		}

		@Override
		public String toString() {
			return "Campaign ID " + id + ": " + "day " + dayStart + " to "
					+ dayEnd + " " + targetSegment + ", reach: " + reachImps
					+ " coefs: (v=" + videoCoef + ", m=" + mobileCoef + ")";
		}

		int impsTogo() {
			return (int) Math.max(0, reachImps - stats.getTargetedImps());
		}

		void setStats(CampaignStats s) {
			stats.setValues(s);
		}

		public AdxQuery[] getCampaignQueries() {
			return campaignQueries;
		}

		public void setCampaignQueries(AdxQuery[] campaignQueries) {
			this.campaignQueries = campaignQueries;
		}

	}

}
