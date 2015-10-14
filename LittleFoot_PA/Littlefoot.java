import java.awt.event.*;

import javax.swing.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.Deck;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.Hand;
import com.biotools.meerkat.HandEvaluator;
import com.biotools.meerkat.Holdem;
import com.biotools.meerkat.Player;
import com.biotools.meerkat.util.Preferences;

class Enums
{
	public enum eMessageType
	{
		eMsgUnknown(0, "eMsgUnknown"),
		// Messages
		eClientKnnRequest(1, "eClientKnnRequest"),
		eServerKnnReply(2, "eServerKnnReply"),
		// Save Hand to DB
		eClientSaveHandRequest(3, "eClientSaveHandRequest"),
		eServerInsertedHandID(4, "eServerInsertedHandID"),
		// Hopper Event
		eHopperRebuyEvent(5, "eHopperRebuyEvent"),
		// Save Profit to DB
		eClientSaveProfit(6, "eClientSaveProfit"),
		eServerInsertedProfitHandID(7, "eServerInsertedProfitHandID"),
		eMsgQuit(8, "eMsgQuit"),
		eTotalMsgs(9, "");

		private eMessageType(int arg1, String arg2)
		{
			this.val1 = arg1;
			this.val2 = arg2;
		}

		final int getVal()
		{
			return this.val1;
		}

		final String getName()
		{
			return this.val2;
		}

		private final int		val1;
		private final String	val2;
	}

	public enum eBets
	{
		eUnknown(-1), //
		eFold(0), //
		eCheck(1), //
		eCall(2), //
		eHalfPot(3), //
		ePot(4), //
		eTwoPots(5), //
		// eAllin(6), //
		eTotalActions(6);

		private eBets(int val)
		{
			this.value = val;
		}

		final int getVal()
		{
			return this.value;
		}

		private final int	value;
	}

	public enum eDims
	{
		// Hand
		RequestType(0), //
		Decision(1), //
		handId(2), //
		isNew(3), //
		brNdx(4), //
		timesActed(5), //
		// dims
		Action1(6), //
		Action2(7), //
		Action3(8), //
		Action4(9), //
		HS(10), //
		InHand(11), //
		Dealt(12), //
		ToAct(13), //
		Balance(14), //
		InitialBalance(15), //
		Call(16), //
		Pot(17), //
		Raises(18), //
		PPot(19), //
		NPot(20), //
		// markers
		Total(21), //
		End(NPot.getVal()), //
		PF_End(Raises.getVal()), //
		Start(HS.getVal());

		private eDims(int val)
		{
			this.value = val;
		}

		final int getVal()
		{
			return this.value;
		}

		private final int	value;
	}

	public enum eProfit
	{
		// Profit
		RequestType(0), //
		handId(1), //
		hwnd(2), //
		// profit
		Amt(3), //
		// markers
		Total(Amt.getVal());

		private eProfit(int val)
		{
			this.value = val;
		}

		final int getVal()
		{
			return this.value;
		}

		private final int	value;
	}
}

public class Littlefoot implements Player
{
	private int									ourSeat;
	private Card								c1, c2;
	private GameInfo							gi;
	private Preferences							prefs;
	private Client								myclient;
	private ConcurrentMap<Long, BigFootState>	handid_map;

	private static final boolean				VERBOSE	= true;
	private int 								initial_balance;

	public Littlefoot() throws IOException
	{
		myclient = null;
		handid_map = new ConcurrentHashMap<Long, BigFootState>();
		initial_balance = 0;
	}

	public void holeCards(Card c1, Card c2, int seat)
	{

		/**
		 * An event called to tell us our hole cards and seat number
		 * 
		 * @param c1 your first hole card
		 * @param c2 your second hole card
		 * @param seat your seat number at the table
		 */

		this.c1 = c1;
		this.c2 = c2;
		this.ourSeat = seat;
	}

	public Action getAction()
	{
		/**
		 * Requests an Action from the player Called when it is the Player's turn to act.
		 */
		 
		if(this.myclient == null)
		{
			try
			{
				this.myclient = new Client(7777, -1, "127.0.0.1", 1);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return Get_Decision();
	}

	public Preferences getPreferences()
	{
		/**
		 * Get the current settings for this bot.
		 */

		return prefs;
	}

	public void init(Preferences playerPrefs)
	{
		/**
		 * Load the current settings for this bot.
		 */

		this.prefs = playerPrefs;
	}

	public boolean getDebug()
	{
		/**
		 * @return true if debug mode is on.
		 */

		return prefs.getBooleanPreference("DEBUG", false);
	}

	public void debug(String str)
	{
		/**
		 * print a debug statement.
		 */

		if (getDebug())
		{
			System.out.println(str);
		}
	}

	public void debugb(String str)
	{
		/**
		 * print a debug statement with no end of line character
		 */

		if (getDebug())
		{
			System.out.print(str);
		}
	}

	public void stageEvent(int stage)
	{
		/**
		 * A new betting round has started.
		 */
	}

	public void showdownEvent(int seat, Card c1, Card c2)
	{
		/**
		 * A showdown has occurred.
		 * 
		 * @param pos the position of the player showing
		 * @param c1 the first hole card shown
		 * @param c2 the second hole card shown
		 */
	}

	public void gameStartEvent(GameInfo gInfo)
	{
		/**
		 * A new game has been started.
		 * 
		 * @param gi the game state information
		 */

		this.gi = gInfo;
	}

	public void dealHoleCardsEvent()
	{
		/**
		 * An event sent when all players are being dealt their hole cards
		 */
	}

	public void actionEvent(int pos, Action act)
	{
		/**
		 * An action has been observed.
		 */
	}

	public void gameStateChanged()
	{
		/**
		 * The game info state has been updated Called after an action event has been fully processed
		 */
	}

	public void gameOverEvent()
	{
		/**
		 * The hand is now over.
		 */

		// check what profit we made
		boolean handid_defined = handid_map.containsKey(gi.getGameID());

		if (handid_defined)
		{
			int[] profit = new int[Enums.eDims.Total.getVal()];
			profit[Enums.eProfit.Amt.getVal()] = (int) ((gi.getBankRoll(ourSeat) / gi.getSmallBlindSize()) - initial_balance);
			profit[Enums.eProfit.handId.getVal()] = handid_map.get(gi.getGameID()).hand_id.intValue();

			if (profit[Enums.eProfit.handId.getVal()] >= 0)
			{
				ExecuteProfitMessageExchange(profit);
				handid_map.remove(gi.getGameID());
			}
			else
				System.out.printf("ERROR - %d HAND_ID INVALID !!!\n", profit[Enums.eProfit.handId.getVal()]);
		}
	}

	public void winEvent(int pos, double amount, String handName)
	{
		/**
		 * A player at pos has won amount with the hand handName
		 */
	}

	private void FillHandArrayPreFlop(int[] hand, BigFootState state)
	{
		// Get betround preflop
		hand[Enums.eDims.brNdx.getVal()] = 0;
		// times acted
		hand[Enums.eDims.timesActed.getVal()] = state.times_acted[0];
		// hand_id
		hand[Enums.eDims.handId.getVal()] = (state.times_acted[0] > 0 ? handid_map.get(gi.getGameID()).hand_id.intValue() : -1);
		// Dealt
		hand[Enums.eDims.Dealt.getVal()] = gi.getNumPlayers();
		// number of players that still need to act
		hand[Enums.eDims.ToAct.getVal()] = gi.getNumToAct();
		// number of players left in the hand (including us) (*)
		hand[Enums.eDims.InHand.getVal()] = gi.getNumActivePlayers();
		// Balance
		hand[Enums.eDims.Balance.getVal()] = (int) (gi.getBankRoll(ourSeat) / gi.getSmallBlindSize());
		// Initial Balance
		hand[Enums.eDims.InitialBalance.getVal()] = initial_balance;
		// amount to call
		hand[Enums.eDims.Call.getVal()] = (int) (gi.getAmountToCall(ourSeat) / gi.getSmallBlindSize());
		// amount in pot
		hand[Enums.eDims.Pot.getVal()] = (int) (gi.getTotalPotSize() / gi.getSmallBlindSize());
		// num raises
		hand[Enums.eDims.Raises.getVal()] = gi.getNumRaises();

		// compute our current hand rank [0...100]
		EnumerateResult result = enumerateHands(c1, c2, gi.getBoard());
		hand[Enums.eDims.HS.getVal()] = (int) (100 * Math.pow(result.HR, hand[Enums.eDims.InHand.getVal()] - 1));
	}

	private void FillHandArrayPostFlop(int[] hand, BigFootState state)
	{
		// constants
		hand[Enums.eDims.brNdx.getVal()] = state.br_ndx;
		hand[Enums.eDims.timesActed.getVal()] = state.times_acted[state.br_ndx];
		// overflow :p
		hand[Enums.eDims.handId.getVal()] = handid_map.get(gi.getGameID()).hand_id.intValue();
		// Dealt
		hand[Enums.eDims.Dealt.getVal()] = gi.getNumPlayers();
		// number of players that still need to act
		hand[Enums.eDims.ToAct.getVal()] = gi.getNumToAct();
		// number of players left in the hand (including us) (*)
		hand[Enums.eDims.InHand.getVal()] = gi.getNumActivePlayers();
		// Initial Balance
		hand[Enums.eDims.InitialBalance.getVal()] = initial_balance;
		// Balance
		hand[Enums.eDims.Balance.getVal()] = (int) (gi.getBankRoll(ourSeat) / gi.getSmallBlindSize());
		// amount to call
		hand[Enums.eDims.Call.getVal()] = (int) (gi.getAmountToCall(ourSeat) / gi.getSmallBlindSize());
		// amount in pot
		hand[Enums.eDims.Pot.getVal()] = (int) (gi.getTotalPotSize() / gi.getSmallBlindSize());
		// num raises
		hand[Enums.eDims.Raises.getVal()] = gi.getNumRaises();

		// compute our current hand rank [0...100]
		EnumerateResult result = enumerateHands(c1, c2, gi.getBoard());
		hand[Enums.eDims.HS.getVal()] = (int) (100 * Math.pow(result.HR, hand[Enums.eDims.InHand.getVal()] - 1));
		// compute a fast approximation of our hand potential, PPot and Npot
		// [0...1]
		hand[Enums.eDims.PPot.getVal()] = 0;
		hand[Enums.eDims.NPot.getVal()] = 0;

		if (gi.getStage() < Holdem.RIVER)
		{
			hand[Enums.eDims.PPot.getVal()] = (int) (100 * result.PPot);
			hand[Enums.eDims.NPot.getVal()] = (int) (100 * result.NPot);
		}
	}

	int ExecuteKnnMessageExchange(int[] hand)
	{
		int bet_type = Enums.eBets.eUnknown.getVal();
		int recv_msgtype = Enums.eMessageType.eMsgUnknown.getVal();
		int[] recv_buffer = new int[Enums.eDims.Total.getVal()];

		// send 1 : knn request - send the hand array directly
		hand[Enums.eDims.RequestType.getVal()] = Enums.eMessageType.eClientKnnRequest.getVal();
		try
		{
			myclient.SendInts(hand, Enums.eDims.Total.getVal());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// recv 2: Wait for eServerKnnReply
		recv_msgtype = Enums.eMessageType.eMsgUnknown.getVal();
		java.util.Arrays.fill(recv_buffer, 0);
		try
		{
			myclient.RecvInts(recv_buffer, Enums.eDims.Total.getVal());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// check if messages are different or of unexpected type
		recv_msgtype = recv_buffer[Enums.eDims.RequestType.getVal()];
		if (recv_msgtype != Enums.eMessageType.eServerKnnReply.getVal())
		{
			System.out.printf("ERROR: Expecting eServerKnnReply [Got %d]\n", recv_msgtype);
			return -1;
		}

		// We are done :)
		bet_type = recv_buffer[Enums.eDims.Decision.getVal()];

		return bet_type;
	}

	int ExecuteHandUpdateMessageExchange(int[] hand)
	{
		int inserted_hand_id = -1;
		int recv_msgtype = Enums.eMessageType.eMsgUnknown.getVal();
		int[] recv_buffer = new int[Enums.eDims.Total.getVal()];

		// SEND 1
		hand[Enums.eDims.RequestType.getVal()] = Enums.eMessageType.eClientSaveHandRequest.getVal();
		try
		{
			myclient.SendInts(hand, Enums.eDims.Total.getVal());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// RECV 2
		java.util.Arrays.fill(recv_buffer, 0);
		try
		{
			myclient.RecvInts(recv_buffer, Enums.eDims.Total.getVal());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// check if messages are different or of unexpected type
		recv_msgtype = recv_buffer[Enums.eDims.RequestType.getVal()];
		if (Enums.eMessageType.eServerInsertedHandID.getVal() != recv_msgtype)
		{
			System.out.printf("ERROR: Expecting eServerInsertedID [Got %d]\n", recv_msgtype);
			return -1;
		}

		inserted_hand_id = recv_buffer[Enums.eDims.handId.getVal()];
		//System.out.printf(">>> Inserted hand %d\n", inserted_hand_id);

		return inserted_hand_id;
	}

	int ExecuteProfitMessageExchange(int[] profit)
	{
		int inserted_hand_id = -1;
		int recv_msgtype;
		int[] recv_buffer = new int[Enums.eDims.Total.getVal()];

		// send 1 : profit request
		profit[Enums.eProfit.RequestType.getVal()] = Enums.eMessageType.eClientSaveProfit.getVal();
		if (handid_map.containsKey(gi.getGameID()))
			profit[Enums.eProfit.handId.getVal()] = (int) handid_map.get(gi.getGameID()).hand_id.intValue();
		else
			profit[Enums.eProfit.handId.getVal()] = -1;

		// SEND 1
		try
		{
			myclient.SendInts(profit, Enums.eDims.Total.getVal());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// RECV 2
		try
		{
			myclient.RecvInts(recv_buffer, Enums.eDims.Total.getVal());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// check if messages are different or of unexpected type
		recv_msgtype = recv_buffer[Enums.eProfit.RequestType.getVal()];
		if (Enums.eMessageType.eServerInsertedProfitHandID.getVal() != recv_msgtype)
		{
			System.out.printf("ERROR: Expecting eServerInsertedID [Got %s]\n", recv_msgtype);
			return -1;
		}

		inserted_hand_id = recv_buffer[Enums.eProfit.handId.getVal()];
		if (VERBOSE)
			System.out.printf("PROFIT : %d\n", recv_buffer[Enums.eProfit.handId.getVal()]);

		return inserted_hand_id;
	}

	private double TranslateActionToAmt(Enums.eBets betType)
	{
		double amt = 0.0;
		double pot = gi.getTotalPotSize();
		double call = gi.getAmountToCall(ourSeat);

		switch (betType)
		{
			case eFold:
				break;
			case eCheck:
				break;
			case eCall:
				amt = call;
				break;
			case eHalfPot:
				amt = call + 0.5 * pot;
				break;
			case ePot:
				amt = call + pot;
				break;
			case eTwoPots:
				amt = call + 2.0 * pot;
				break;
			default:
				break;
		}

		return (amt > gi.getBankRoll(ourSeat) ? gi.getBankRoll(ourSeat) : amt);
	}

	private int GetBR()
	{
		if (gi.isPreFlop())
			return 0;
		else if (gi.isFlop())
			return 1;
		else if (gi.isTurn())
			return 2;
		else if (gi.isRiver())
			return 3;

		return -1;
	}

	private Action Get_Decision()
	{

		Action ret;
		int[] hand = new int[Enums.eDims.Total.getVal()];

		BigFootState bf_state = null;
		if (handid_map.containsKey(gi.getGameID()))
			bf_state = handid_map.get(gi.getGameID());
		else
			bf_state = new BigFootState();

		bf_state.br_ndx = GetBR();

		// Fill hand[]
		if (0 == bf_state.br_ndx)
		{
			if(0 == bf_state.times_acted[0])
				initial_balance = (int)(gi.getBankRoll(ourSeat) / gi.getSmallBlindSize());
				
			FillHandArrayPreFlop(hand, bf_state);
		}
		else
			FillHandArrayPostFlop(hand, bf_state);

		// previous actions
		if (Math.min(hand[Enums.eDims.timesActed.getVal()], 3) >= 1)
		{
			for (int i = 0; i < Math.min(hand[Enums.eDims.timesActed.getVal()], 4); i++)
				hand[Enums.eDims.Action1.getVal() + i] = bf_state.curr_actions[i];
		}

		// Get decision from master
		int dec = Enums.eBets.eUnknown.getVal();
		dec = ExecuteKnnMessageExchange(hand);

		// hmmm ... will lead to problems - FIX ME
		if (Enums.eBets.eCheck.getVal() >= dec)
			ret = Action.checkOrFoldAction(gi.getAmountToCall(ourSeat));
		else if (Enums.eBets.eCall.getVal() == dec)
			ret = Action.callAction(gi);
		else
		{
			double wantedRaiseAmount = TranslateActionToAmt(Enums.eBets.values()[dec]);
			ret = Action.raiseAction(gi, wantedRaiseAmount);
		}

		// Stupid fix for when times_acted >= k_max_actions
		// Just call here and avoid errors
		if (bf_state.times_acted[bf_state.br_ndx] == 3 && dec > Enums.eBets.eCall.getVal())
		{
			dec = Enums.eBets.eCall.getVal();
			ret = Action.callAction(gi);
		}

		// current action
		hand[Enums.eDims.Action1.getVal() + Math.min(hand[Enums.eDims.timesActed.getVal()], 3)] = dec;

		// Save hand
		bf_state.hand_id = (long) ExecuteHandUpdateMessageExchange(hand);
		bf_state.curr_actions[bf_state.times_acted[bf_state.br_ndx]] = dec;
		bf_state.times_acted[bf_state.br_ndx]++;
		bf_state.new_hand = false;

		if (VERBOSE)
			System.out.printf(">>> %d - hand_id: %d\n", gi.getGameID(), (long) bf_state.hand_id);

		if (!handid_map.containsKey(gi.getGameID()))
		{
			if (bf_state.hand_id > 0)
				handid_map.put(gi.getGameID(), bf_state);
			else
				System.out.printf("ERROR - Invalid Hand ID Saving hand: %d", bf_state.hand_id);
		}

		return ret;

	}

	/**
	 * Calculate the raw (unweighted) PPot1 and NPot1 of a hand. (Papp 1998, 5.3)
	 * Does a one-card look ahead.
	 * 
	 * @param c1 the first hole card
	 * @param c2 the second hole card
	 * @param bd the board cards
	 * @return the ppot (also sets npot not returned)
	 */
	public EnumerateResult enumerateHands(Card c1, Card c2, Hand bd)
	{
		double[][] HP = new double[3][3];
		double[] HPTotal = new double[3];
		int ourrank7, opprank;
		int index;
		Hand board = new Hand(bd);
		int ourrank5 = HandEvaluator.rankHand(c1, c2, bd);

		// remove all known cards
		Deck d = new Deck();
		d.extractCard(c1);
		d.extractCard(c2);
		d.extractHand(board);

		// pick first opponent card
		for (int i = d.getTopCardIndex(); i < Deck.NUM_CARDS; i++)
		{
			Card o1 = d.getCard(i);
			// pick second opponent card
			for (int j = i + 1; j < Deck.NUM_CARDS; j++)
			{
				Card o2 = d.getCard(j);
				opprank = HandEvaluator.rankHand(o1, o2, bd);

				if (ourrank5 > opprank)
					index = AHEAD;
				else if (ourrank5 == opprank)
					index = TIED;
				else
					index = BEHIND;

				HPTotal[index]++;

				// tally all possiblities for next board card
				for (int k = d.getTopCardIndex(); k < Deck.NUM_CARDS; k++)
				{
					if (i == k || j == k)
						continue;

					board.addCard(d.getCard(k));

					ourrank7 = HandEvaluator.rankHand(c1, c2, board);
					opprank = HandEvaluator.rankHand(o1, o2, board);

					if (ourrank7 > opprank)
						HP[index][AHEAD]++;
					else if (ourrank7 == opprank)
						HP[index][TIED]++;
					else
						HP[index][BEHIND]++;

					board.removeCard();
				}
			}
		} /* end of possible opponent hands */

		double ppot = 0, npot = 0;
		double den1 = (45 * (HPTotal[BEHIND] + (HPTotal[TIED] / 2.0)));
		double den2 = (45 * (HPTotal[AHEAD] + (HPTotal[TIED] / 2.0)));
		EnumerateResult result = new EnumerateResult();
		if (den1 > 0)
		{
			result.PPot = (HP[BEHIND][AHEAD] + (HP[BEHIND][TIED] / 2.0) + (HP[TIED][AHEAD] / 2.0)) / (double) den1;
		}
		if (den2 > 0)
		{
			result.NPot = (HP[AHEAD][BEHIND] + (HP[AHEAD][TIED] / 2.0) + (HP[TIED][BEHIND] / 2.0)) / (double) den2;
		}
		result.HR = (HPTotal[AHEAD] + (HPTotal[TIED] / 2)) / (HPTotal[AHEAD] + HPTotal[TIED] + HPTotal[BEHIND]);

		return result;
	}

	// constants used in above method:
	private final static int	AHEAD	= 0;
	private final static int	TIED	= 1;
	private final static int	BEHIND	= 2;

	class EnumerateResult
	{
		double	HR;
		double	PPot;
		double	NPot;
	}

	class BigFootState
	{
		Long	hand_id;
		int		br_ndx;
		int		times_acted[];
		int		curr_actions[];
		boolean	new_hand;

		public BigFootState()
		{
			hand_id = -1L;
			br_ndx = 0;
			times_acted = new int[4];
			curr_actions = new int[4];
			java.util.Arrays.fill(times_acted, 0);
			java.util.Arrays.fill(curr_actions, 0);
			new_hand = true;
		}
	}

	public class Client
	{

		boolean					VERBOSE		= true;	// turn on/off debugging output
		final int				BUFFSIZE	= 128000;	// how many bytes our incoming buffer can hold

		byte					data[];
		byte					buff[];

		int						port;
		int						dataport;
		String					host;
		Socket					sock;
		DatagramSocket			recv_sock, send_sock;

		BufferedInputStream		input;
		BufferedOutputStream	output;

		// constructor, takes a port number, a machine name host, and a bit
		// that indicates whether to reverse the byte order or not
		public Client(int p, int datap, String address, int rev) throws IOException
		{
			port = p;
			dataport = datap;
			host = address;

			try
			{
				sock = new Socket(InetAddress.getByName(address), port);
				input = new BufferedInputStream(sock.getInputStream(), BUFFSIZE);
				output = new BufferedOutputStream(sock.getOutputStream(), BUFFSIZE);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			if (dataport != -1)
			{
				// allocate the datagram socket
				try
				{
					recv_sock = new DatagramSocket(dataport);
					send_sock = new DatagramSocket();
				}
				catch (SocketException se)
				{
					se.printStackTrace();
				}
			}

			// amortize the buffer allocation by just doing it once
			buff = new byte[BUFFSIZE];
			data = new byte[BUFFSIZE];

			if (VERBOSE)
				System.out.println("Client: opening socket to " + address + " on port " + port + ", datagrams on port = " + dataport);

			// now we want to tell the server if we want reversed bytes or not

			/*
			output.write(rev);
			output.flush();
			*/

			if (VERBOSE)
			{
				if (rev == 1)
					System.out.println("Client:  requested reversed bytes");
				else
					System.out.println("Client:  requested normal byte order");
			}
		}

		// send a string down the socket
		public void SendString(String str) throws IOException
		{

			/* convert our string into an array of bytes */
			ByteArrayOutputStream bytestream;
			bytestream = new ByteArrayOutputStream(str.length());

			DataOutputStream out;
			out = new DataOutputStream(bytestream);

			for (int i = 0; i < str.length(); i++)
				out.write((byte) str.charAt(i));

			output.write(bytestream.toByteArray(), 0, bytestream.size());
			output.flush();

			if (VERBOSE)
				System.out.println("Client: sending '" + str + "'");

			RecvAck();
			SendAck();
		}

		public void SendBytes(byte vals[], int len) throws IOException
		{
			if (VERBOSE)
			{
				System.out.print("Client: sending " + len + " bytes: ");
				for (int i = 0; i < len; i++)
					System.out.print(vals[i] + " ");
			}

			output.write(vals, 0, len);
			output.flush();

			if (VERBOSE)
				System.out.println("");

			RecvAck();
			SendAck();
		}

		public void SendInts(int vals[], int len) throws IOException
		{
			if (VERBOSE)
				System.out.print("Client: sending " + len + " ints: ");

			/* convert our array of ints into an array of bytes */
			ByteArrayOutputStream bytestream;
			bytestream = new ByteArrayOutputStream(len * 4);

			DataOutputStream out;
			out = new DataOutputStream(bytestream);

			for (int i = 0; i < len; i++)
			{
				out.writeInt(vals[i]);
				if (VERBOSE)
					System.out.print(vals[i] + " ");
			}

			output.write(bytestream.toByteArray(), 0, bytestream.size());
			output.flush();

			if (VERBOSE)
				System.out.println("");

			RecvAck();
			SendAck();
		}

		public void SendFloats(float vals[], int len) throws IOException
		{
			if (VERBOSE)
				System.out.print("Client: sending " + len + " floats: ");

			/* convert our array of floats into an array of bytes */
			ByteArrayOutputStream bytestream;
			bytestream = new ByteArrayOutputStream(len * 4);

			DataOutputStream out;
			out = new DataOutputStream(bytestream);

			for (int i = 0; i < len; i++)
			{
				out.writeFloat(vals[i]);
				if (VERBOSE)
					System.out.print(vals[i] + " ");
			}

			output.write(bytestream.toByteArray(), 0, bytestream.size());
			output.flush();

			if (VERBOSE)
				System.out.println("");

			RecvAck();
			SendAck();
		}

		public void SendDoubles(double vals[], int len) throws IOException
		{
			if (VERBOSE)
				System.out.print("Client: sending " + len + " doubles: ");

			/* convert our array of floats into an array of bytes */
			ByteArrayOutputStream bytestream;
			bytestream = new ByteArrayOutputStream(len * 8);

			DataOutputStream out;
			out = new DataOutputStream(bytestream);

			for (int i = 0; i < len; i++)
			{
				out.writeDouble(vals[i]);
				if (VERBOSE)
					System.out.print(vals[i] + " ");
			}

			output.write(bytestream.toByteArray(), 0, bytestream.size());
			output.flush();

			if (VERBOSE)
				System.out.println("");

			RecvAck();
			SendAck();
		}

		public void SendDatagram(byte vals[], int len) throws IOException
		{
			DatagramPacket sendPacket;

			if (VERBOSE)
			{
				System.out.print("Client: sending datagram of " + len + " bytes: ");
				for (int i = 0; i < len; i++)
					System.out.print(vals[i] + " ");
			}

			sendPacket = new DatagramPacket(vals, len, InetAddress.getByName(host), dataport);
			send_sock.send(sendPacket);

			if (VERBOSE)
				System.out.println("");

		}

		// recv a string from the socket (terminates on terminal char)
		public String RecvString(char terminal) throws IOException
		{
			char c;
			String out;

			// would have liked to use readUTF, but it didn't seem to work
			// when talking to the c++ server
			out = new String("");

			while ((c = (char) input.read()) != terminal)
				out = out + String.valueOf(c);

			if (VERBOSE)
				System.out.println("Client: recv'd '" + out + "'");

			SendAck();
			RecvAck();

			return out;
		}

		public int RecvBytes(byte val[], int maxlen) throws IOException
		{
			int i;
			int totalbytes = 0;
			int numbytes;

			if (maxlen > BUFFSIZE)
				System.out.println("Sending more bytes then will fit in buffer!");

			while (totalbytes < maxlen)
			{
				numbytes = input.read(data);

				// copy the bytes into the result buffer
				for (i = totalbytes; i < totalbytes + numbytes; i++)
					val[i] = data[i - totalbytes];

				totalbytes += numbytes;
			}

			if (VERBOSE)
			{
				System.out.print("Client: received " + maxlen + " bytes - ");
				for (i = 0; i < maxlen; i++)
					System.out.print(val[i] + " ");
				System.out.println("");
			}

			// we now send an acknowledgement to the server to let them know we've got it
			SendAck();
			RecvAck();

			return maxlen;
		}

		public int RecvInts(int val[], int maxlen) throws IOException
		{
			int i;
			int totalbytes = 0;
			int numbytes;

			/* for performance, we need to receive data as an array of bytes
				and then convert to an array of ints, more fun than
				you can shake a stick at! */

			if (maxlen * 4 > BUFFSIZE)
				System.out.println("Sending more ints then will fit in buffer!");

			while (totalbytes < maxlen * 4)
			{
				numbytes = input.read(data);

				// copy the bytes into the result buffer
				for (i = totalbytes; i < totalbytes + numbytes; i++)
					buff[i] = data[i - totalbytes];

				totalbytes += numbytes;
			}

			// now we must convert the array of bytes to an array of ints
			ByteArrayInputStream bytestream;
			DataInputStream instream;

			bytestream = new ByteArrayInputStream(buff);
			instream = new DataInputStream(bytestream);

			for (i = 0; i < maxlen; i++)
				val[i] = instream.readInt();

			if (VERBOSE)
			{
				System.out.print("Client: received " + maxlen + " ints - ");
				for (i = 0; i < maxlen; i++)
					System.out.print(val[i] + " ");
				System.out.println("");
			}

			// we now send an acknowledgement to the server to let them
			// know we've got it
			SendAck();
			RecvAck();

			return maxlen;
		}

		public int RecvDoubles(double val[], int maxlen) throws IOException
		{
			int i;
			int numbytes;
			int totalbytes = 0;

			/* for performance, we need to receive data as an array of bytes
				and then convert to an array of ints, more fun than
				you can shake a stick at! */

			if (maxlen * 8 > BUFFSIZE)
				System.out.println("Sending more doubles then will fit in buffer!");

			while (totalbytes < maxlen * 8)
			{
				numbytes = input.read(data);

				// copy the bytes into the result buffer
				for (i = totalbytes; i < totalbytes + numbytes; i++)
					buff[i] = data[i - totalbytes];

				totalbytes += numbytes;

			}

			// now we must convert the array of bytes to an array of ints
			ByteArrayInputStream bytestream;
			DataInputStream instream;

			bytestream = new ByteArrayInputStream(buff);
			instream = new DataInputStream(bytestream);

			for (i = 0; i < maxlen; i++)
				val[i] = instream.readDouble();

			if (VERBOSE)
			{
				System.out.print("Client: received " + maxlen + " doubles - ");
				for (i = 0; i < maxlen; i++)
					System.out.print(val[i] + " ");
				System.out.println("");
			}

			SendAck();
			RecvAck();

			return maxlen;
		}

		public int RecvFloats(float val[], int maxlen) throws IOException
		{
			int i;
			int numbytes;
			int totalbytes = 0;

			/* for performance, we need to receive data as an array of bytes
				and then convert to an array of ints, more fun than
				you can shake a stick at! */

			if (maxlen * 4 > BUFFSIZE)
				System.out.println("Sending more doubles then will fit in buffer!");

			while (totalbytes < maxlen * 4)
			{
				numbytes = input.read(data);

				// copy the bytes into the result buffer
				for (i = totalbytes; i < totalbytes + numbytes; i++)
					buff[i] = data[i - totalbytes];

				totalbytes += numbytes;

			}

			// now we must convert the array of bytes to an array of ints
			ByteArrayInputStream bytestream;
			DataInputStream instream;

			bytestream = new ByteArrayInputStream(buff);
			instream = new DataInputStream(bytestream);

			for (i = 0; i < maxlen; i++)
				val[i] = instream.readFloat();

			if (VERBOSE)
			{
				System.out.print("Client: received " + maxlen + " floats - ");
				for (i = 0; i < maxlen; i++)
					System.out.print(val[i] + " ");
				System.out.println("");
			}

			SendAck();
			RecvAck();

			return maxlen;
		}

		public int RecvDatagram(byte val[], int maxlen) throws IOException
		{
			int i;
			int numbytes;
			DatagramPacket receivePacket;

			if (maxlen > BUFFSIZE)
				System.out.println("Sending more bytes then will fit in buffer!");

			receivePacket = new DatagramPacket(val, maxlen);
			recv_sock.receive(receivePacket);

			numbytes = receivePacket.getLength();

			if (VERBOSE)
			{
				System.out.print("Client: received " + numbytes + " bytes - ");
				for (i = 0; i < numbytes; i++)
					System.out.print(val[i] + " ");
				System.out.println("");
			}

			return numbytes;
		}

		// shutdown the socket
		public void Close() throws IOException
		{
			sock.close();

			if (VERBOSE)
				System.out.println("Client: closing socket");
		}

		// send a short ack to the server so they know we are ready for more
		private void SendAck() throws IOException
		{
			int ack;

			ack = 0;

			if (VERBOSE)
				System.out.println("Sending ack...");

			output.write(ack);
			output.flush();

		}

		// recv a short ack from the server so we know they are ready for more
		private void RecvAck() throws IOException
		{
			int ack;

			if (VERBOSE)
				System.out.println("Waiting for ack...");

			ack = (int) input.read();

			if (VERBOSE)
				System.out.println("Ack recieved.");

		}
	}

}