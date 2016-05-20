package l2d.game.instancemanager;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.Config;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.ThreadPoolManager;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Manor;
import l2d.game.model.Warehouse;
import l2d.game.model.entity.residence.Castle;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ClanTable;
import l2d.game.tables.ItemTable;
import l2d.util.Log;
import l2d.util.Rnd;

public class CastleManorManager
{
	protected static Logger _log = Logger.getLogger(CastleManorManager.class.getName());

	private static CastleManorManager _instance;

	public static final int PERIOD_CURRENT = 0;
	public static final int PERIOD_NEXT = 1;
	protected static final String var_name = "ManorApproved";

	private static final String CASTLE_MANOR_LOAD_PROCURE = "SELECT * FROM castle_manor_procure WHERE castle_id=?";
	private static final String CASTLE_MANOR_LOAD_PRODUCTION = "SELECT * FROM castle_manor_production WHERE castle_id=?";

	private static final int NEXT_PERIOD_APPROVE = Config.MANOR_APPROVE_TIME; // 6:00
	private static final int NEXT_PERIOD_APPROVE_MIN = Config.MANOR_APPROVE_MIN; //
	private static final int MANOR_REFRESH = Config.MANOR_REFRESH_TIME; // 20:00
	private static final int MANOR_REFRESH_MIN = Config.MANOR_REFRESH_MIN; //
	protected static final long MAINTENANCE_PERIOD = Config.MANOR_MAINTENANCE_PERIOD / 60000; // 6 mins

	private boolean _underMaintenance;
	private boolean _disabled;

	public static CastleManorManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Manor System: Initializing...");
			_instance = new CastleManorManager();
		}
		return _instance;
	}

	public class CropProcure
	{
		int _cropId;
		int _buyResidual;
		int _rewardType;
		int _buy;
		int _price;

		public CropProcure(int id)
		{
			_cropId = id;
			_buyResidual = 0;
			_rewardType = 0;
			_buy = 0;
			_price = 0;
		}

		public CropProcure(int id, int amount, int type, int buy, int price)
		{
			_cropId = id;
			_buyResidual = amount;
			_rewardType = type;
			_buy = buy;
			_price = price;
		}

		public int getReward()
		{
			return _rewardType;
		}

		public int getId()
		{
			return _cropId;
		}

		public int getAmount() //TODO: long
		{
			return _buyResidual;
		}

		public int getStartAmount() //TODO: long
		{
			return _buy;
		}

		public int getPrice() //TODO: long
		{
			return _price;
		}

		public void setAmount(int amount)
		{
			_buyResidual = amount;
		}
	}

	public class SeedProduction
	{
		int _seedId;
		int _residual;
		int _price;
		int _sales;

		public SeedProduction(int id)
		{
			_seedId = id;
			_sales = 0;
			_price = 0;
			_sales = 0;
		}

		public SeedProduction(int id, int amount, int price, int sales)
		{
			_seedId = id;
			_residual = amount;
			_price = price;
			_sales = sales;
		}

		public int getId()
		{
			return _seedId;
		}

		public int getCanProduce() //TODO: long
		{
			return _residual;
		}

		public int getPrice() //TODO: long
		{
			return _price;
		}

		public int getStartProduce() //TODO: long
		{
			return _sales;
		}

		public void setCanProduce(int amount)
		{
			_residual = amount;
		}
	}

	private CastleManorManager()
	{
		load(); // load data from database
		init(); // schedule all manor related events
		_underMaintenance = false;
		_disabled = !Config.ALLOW_MANOR;
		for(Castle c : CastleManager.getInstance().getCastles().values())
			c.setNextPeriodApproved(ServerVariables.getBool(var_name));
	}

	private void load()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			// Get Connection
			con = L2DatabaseFactory.getInstance().getConnection();
			for(Castle castle : CastleManager.getInstance().getCastles().values())
			{
				FastList<SeedProduction> production = new FastList<SeedProduction>();
				FastList<SeedProduction> productionNext = new FastList<SeedProduction>();
				FastList<CropProcure> procure = new FastList<CropProcure>();
				FastList<CropProcure> procureNext = new FastList<CropProcure>();

				// restore seed production info
				statement = con.prepareStatement(CASTLE_MANOR_LOAD_PRODUCTION);
				statement.setInt(1, castle.getId());
				rs = statement.executeQuery();
				while(rs.next())
				{
					int seedId = rs.getInt("seed_id");
					int canProduce = rs.getInt("can_produce");
					int startProduce = rs.getInt("start_produce");
					int price = rs.getInt("seed_price");
					int period = rs.getInt("period");
					if(period == PERIOD_CURRENT)
						production.add(new SeedProduction(seedId, canProduce, price, startProduce));
					else
						productionNext.add(new SeedProduction(seedId, canProduce, price, startProduce));
				}

				DatabaseUtils.closeDatabaseSR(statement, rs);

				castle.setSeedProduction(production, PERIOD_CURRENT);
				castle.setSeedProduction(productionNext, PERIOD_NEXT);

				// restore procure info
				statement = con.prepareStatement(CASTLE_MANOR_LOAD_PROCURE);
				statement.setInt(1, castle.getId());
				rs = statement.executeQuery();
				while(rs.next())
				{
					int cropId = rs.getInt("crop_id");
					int canBuy = rs.getInt("can_buy");
					int startBuy = rs.getInt("start_buy");
					int rewardType = rs.getInt("reward_type");
					int price = rs.getInt("price");
					int period = rs.getInt("period");
					if(period == PERIOD_CURRENT)
						procure.add(new CropProcure(cropId, canBuy, rewardType, startBuy, price));
					else
						procureNext.add(new CropProcure(cropId, canBuy, rewardType, startBuy, price));
				}

				castle.setCropProcure(procure, PERIOD_CURRENT);
				castle.setCropProcure(procureNext, PERIOD_NEXT);

				if(!procure.isEmpty() || !procureNext.isEmpty() || !production.isEmpty() || !productionNext.isEmpty())
					_log.info("Manor System: Loaded data for " + castle.getName() + " castle");

				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			DatabaseUtils.closeDatabaseCSR(con, statement, rs);
		}
		catch(Exception e)
		{
			_log.info("Manor System: Error restoring manor data: " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rs);
		}
	}

	protected void init()
	{
		if(ServerVariables.getString(var_name, "").isEmpty())
		{
			Calendar manorRefresh = Calendar.getInstance();
			manorRefresh.set(Calendar.HOUR_OF_DAY, MANOR_REFRESH);
			manorRefresh.set(Calendar.MINUTE, MANOR_REFRESH_MIN);
			manorRefresh.set(Calendar.SECOND, 0);
			manorRefresh.set(Calendar.MILLISECOND, 0);

			Calendar periodApprove = Calendar.getInstance();
			periodApprove.set(Calendar.HOUR_OF_DAY, NEXT_PERIOD_APPROVE);
			periodApprove.set(Calendar.MINUTE, NEXT_PERIOD_APPROVE_MIN);
			periodApprove.set(Calendar.SECOND, 0);
			periodApprove.set(Calendar.MILLISECOND, 0);
			boolean isApproved = periodApprove.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && manorRefresh.getTimeInMillis() > Calendar.getInstance().getTimeInMillis();
			ServerVariables.set(var_name, isApproved);
		}

		Calendar FirstDelay = Calendar.getInstance();
		FirstDelay.set(Calendar.SECOND, 0);
		FirstDelay.set(Calendar.MILLISECOND, 0);
		FirstDelay.add(Calendar.MINUTE, 1);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ManorTask(), FirstDelay.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 60000);
	}

	public void setNextPeriod()
	{
		for(Castle c : CastleManager.getInstance().getCastles().values())
		{
			if(c.getOwnerId() <= 0)
				continue;

			L2Clan clan = ClanTable.getInstance().getClan(c.getOwnerId());
			if(clan == null)
				continue;

			Warehouse cwh = clan.getWarehouse();

			for(CropProcure crop : c.getCropProcure(PERIOD_CURRENT))
			{
				if(crop.getStartAmount() == 0)
					continue;

				// adding bought crops to clan warehouse
				if(crop.getStartAmount() > crop.getAmount())
				{
					_log.info("Manor System: Start Amount of Crop " + crop.getStartAmount() + " > Amount of currnt " + crop.getAmount());
					int count = crop.getStartAmount() - crop.getAmount();

					count = count * 90 / 100;
					if(count < 1 && Rnd.get(99) < 90)
						count = 1;

					if(count >= 1)
					{
						int id = L2Manor.getInstance().getMatureCrop(crop.getId());
						L2ItemInstance i = ItemTable.getInstance().createItem(id);
						i.setCount(count);
						cwh.addItem(i, null);
					}
				}

				// reserved and not used money giving back to treasury
				if(crop.getAmount() > 0)
				{
					c.addToTreasuryNoTax(crop.getAmount() * crop.getPrice(), false, false);
					Log.add(c.getName() + "|" + crop.getAmount() * crop.getPrice() + "|ManorManager", "treasury");
				}

				c.setCollectedShops(0);
				c.setCollectedSeed(0);
			}

			c.setSeedProduction(c.getSeedProduction(PERIOD_NEXT), PERIOD_CURRENT);
			c.setCropProcure(c.getCropProcure(PERIOD_NEXT), PERIOD_CURRENT);

			int manor_cost = c.getManorCost(PERIOD_CURRENT);
			if(c.getTreasury() < manor_cost)
			{
				c.setSeedProduction(getNewSeedsList(c.getId()), PERIOD_NEXT);
				c.setCropProcure(getNewCropsList(c.getId()), PERIOD_NEXT);
				manor_cost = c.getManorCost(PERIOD_CURRENT);
				if(manor_cost > 0)
					Log.add(c.getName() + "|" + -manor_cost + "|ManorManager Error@setNextPeriod", "treasury");
			}
			else
			{
				FastList<SeedProduction> production = new FastList<SeedProduction>();
				FastList<CropProcure> procure = new FastList<CropProcure>();
				for(SeedProduction s : c.getSeedProduction(PERIOD_CURRENT))
				{
					s.setCanProduce(s.getStartProduce());
					production.add(s);
				}
				for(CropProcure cr : c.getCropProcure(PERIOD_CURRENT))
				{
					cr.setAmount(cr.getStartAmount());
					procure.add(cr);
				}
				c.setSeedProduction(production, PERIOD_NEXT);
				c.setCropProcure(procure, PERIOD_NEXT);
			}

			if(Config.MANOR_SAVE_ALL_ACTIONS)
			{
				c.saveCropData();
				c.saveSeedData();
			}

			// Sending notification to a clan leader
			PlayerMessageStack.getInstance().mailto(clan.getLeaderId(), new SystemMessage(SystemMessage.THE_MANOR_INFORMATION_HAS_BEEN_UPDATED));

			c.setNextPeriodApproved(false);
		}
	}

	public void approveNextPeriod()
	{
		for(Castle c : CastleManager.getInstance().getCastles().values())
		{
			// Castle has no owner
			if(c.getOwnerId() <= 0)
				continue;

			int manor_cost = c.getManorCost(PERIOD_NEXT);

			if(c.getTreasury() < manor_cost)
			{
				c.setSeedProduction(getNewSeedsList(c.getId()), PERIOD_NEXT);
				c.setCropProcure(getNewCropsList(c.getId()), PERIOD_NEXT);
				manor_cost = c.getManorCost(PERIOD_NEXT);
				if(manor_cost > 0)
					Log.add(c.getName() + "|" + -manor_cost + "|ManorManager Error@approveNextPeriod", "treasury");
				L2Clan clan = ClanTable.getInstance().getClan(c.getOwnerId());
				PlayerMessageStack.getInstance().mailto(clan.getLeaderId(), new SystemMessage(SystemMessage.THE_AMOUNT_IS_NOT_SUFFICIENT_AND_SO_THE_MANOR_IS_NOT_IN_OPERATION));
			}
			else
			{
				c.addToTreasuryNoTax(-manor_cost, false, false);
				Log.add(c.getName() + "|" + -manor_cost + "|ManorManager", "treasury");
			}
			c.setNextPeriodApproved(true);
		}
	}

	private FastList<SeedProduction> getNewSeedsList(int castleId)
	{
		FastList<SeedProduction> seeds = new FastList<SeedProduction>();
		FastList<Integer> seedsIds = L2Manor.getInstance().getSeedsForCastle(castleId);
		for(int sd : seedsIds)
			seeds.add(new SeedProduction(sd));
		return seeds;
	}

	private FastList<CropProcure> getNewCropsList(int castleId)
	{
		FastList<CropProcure> crops = new FastList<CropProcure>();
		FastList<Integer> cropsIds = L2Manor.getInstance().getCropsForCastle(castleId);
		for(int cr : cropsIds)
			crops.add(new CropProcure(cr));
		return crops;
	}

	public boolean isUnderMaintenance()
	{
		return _underMaintenance;
	}

	public void setUnderMaintenance(boolean mode)
	{
		_underMaintenance = mode;
	}

	public boolean isDisabled()
	{
		return _disabled;
	}

	public void setDisabled(boolean mode)
	{
		_disabled = mode;
	}

	public SeedProduction getNewSeedProduction(int id, int amount, int price, int sales)
	{
		return new SeedProduction(id, amount, price, sales);
	}

	public CropProcure getNewCropProcure(int id, int amount, int type, int price, int buy)
	{
		return new CropProcure(id, amount, type, buy, price);
	}

	public void save()
	{
		for(Castle c : CastleManager.getInstance().getCastles().values())
		{
			c.saveSeedData();
			c.saveCropData();
		}
	}

	private class ManorTask implements Runnable
	{
		@Override
		public void run()
		{
			int H = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			int M = Calendar.getInstance().get(Calendar.MINUTE);

			if(ServerVariables.getBool(var_name)) // 06:00 - 20:00 
			{
				if(H < NEXT_PERIOD_APPROVE || H > MANOR_REFRESH || H == MANOR_REFRESH && M >= MANOR_REFRESH_MIN)
				{
					ServerVariables.set(var_name, false);
					setUnderMaintenance(true);
					_log.info("Manor System: Under maintenance mode started");
				}
			}
			else if(isUnderMaintenance()) // 20:00 - 20:06
			{
				if(H != MANOR_REFRESH || M >= MANOR_REFRESH_MIN + MAINTENANCE_PERIOD)
				{
					setUnderMaintenance(false);
					_log.info("Manor System: Next period started");
					if(isDisabled())
						return;
					setNextPeriod();
					try
					{
						save();
					}
					catch(Exception e)
					{
						_log.info("Manor System: Failed to save manor data: " + e);
					}
				}
			}
			else if(H > NEXT_PERIOD_APPROVE && H < MANOR_REFRESH || H == NEXT_PERIOD_APPROVE && M >= NEXT_PERIOD_APPROVE_MIN)
			{
				ServerVariables.set(var_name, true);
				_log.info("Manor System: Next period approved");
				if(isDisabled())
					return;
				approveNextPeriod();
			}
		}
	}
}
