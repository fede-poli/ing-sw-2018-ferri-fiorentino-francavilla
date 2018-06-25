package it.polimi.se2018.controller;

import it.polimi.se2018.model.*;
import it.polimi.se2018.network.ClientGatherer;
import it.polimi.se2018.network.ClientInfo;
import it.polimi.se2018.utils.Timer;
import it.polimi.se2018.view.ViewInterface;
import it.polimi.se2018.view.VirtualView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class ServerController {
    //Attributes
    public static final Object lock = new Object();
    private Model model;
    private VirtualView view;
    private ClientGatherer clientGatherer;
    private ArrayList<PlayerAction> playerActions = new ArrayList<>();
    private boolean lobbyGathering = true; // TODO: ?? model.started
    //private int numClients = 0;
    private int lobbyTimeout;
    private ResourceLoader resourceLoader = new ResourceLoader();

    //Methods
    public ServerController(Model model, VirtualView view){
        this.model = model;
        this.view = view;
        clientGatherer = new ClientGatherer();
        lobbyTimeout = 10; // TODO: load lobbyTimer value from file;
    }

    private void listenPlayerActions() throws InterruptedException {
        Iterator<PlayerAction> iterator;
        while(!lobbyGathering) {
            while(!updatedPlayerActions()
               && emptyPreLobby()) {
                synchronized (lock) {
                    lock.wait();
                }
            }
            iterator = playerActions.iterator();
            while(iterator.hasNext()) {
                PlayerAction pa = iterator.next();
                if(pa.isUpdated()) {
                    if(validAction(pa)) {
                        performAction(pa);
                    }
                    pa.clear();
                }
            }
            gatherPlayers();
        }
    }

    private boolean updatedPlayerActions() {
        for(PlayerAction pa : playerActions) {
            if(pa.isUpdated()) {
                return true;
            }
        }
        return false;
    }

    private void gatherPlayers() throws InterruptedException {
        synchronized (lock) {
            Timer lobbyTimer = new Timer(10, lock);
            do {
                System.out.println("1 WAIT");
                while(lobbyGathering
                        && emptyPreLobby()
                        && !lobbyTimer.isTimeout()) {
                    lock.wait();
                }
                System.out.println("2 WAIT");
                // check if other players are still connected by clientinfo
                System.out.println("check +");
                checkConnections();
                System.out.println("check -");
                if(lobbyTimer.isTimeout()) {
                    if(model.getNumPlayers() >= 2) {
                        lobbyGathering = false;
                    }
                    else {
                        lobbyTimer = new Timer(lobbyTimeout, lock);
                    }
                }
                // read prelobby clients
                Iterator<ClientInfo> iterator = clientGatherer.iterator();
                while(iterator.hasNext()) {
                    ClientInfo clientInfo = iterator.next();
                    PlayerAction pa = clientInfo.getPlayerAction();
                    if(pa.isUpdated()) {
                        boolean available = true;
                        int i = 0;
                        String username = pa.getUsernameReq();
                        //check username and remove client from prelobby if username valid
                        while(available && (i < model.getNumPlayers())) {
                            if(model.getPlayer(i).getUsername().equals(username)) {
                                available = false;
                            }
                            else {
                                i++;
                            }
                        }
                        if(lobbyGathering && available) {
                            //add client to lobby as players
                            System.out.println("PLAYER");
                            model.addPlayer(username, clientInfo.getLocalModel());
                            System.out.println(username + "joined the game");
                            view.addClient(clientInfo.getView());
                            playerActions.add(pa);
                            //clientGatherer.remove(clientInfo);
                            iterator.remove();
                            //if there are exactly 2 players start the lobbyTimer
                            if(model.getNumPlayers() == 2) {
                                lobbyTimer.start();
                            }
                            else if(model.getNumPlayers() == 4) {
                                lobbyGathering = false;
                            }
                        }
                        else if(!available && !model.getPlayer(i).isConnected()) {
                            System.out.println("REINSERTED");
                            model.reinsertPlayer(i, clientInfo.getLocalModel());
                            view.reinsertClient(i, clientInfo.getView());
                            playerActions.set(i, pa);
                            iterator.remove();
                        }
                        System.out.println("+");
                        pa.clear();
                        System.out.println("-");
                    }
                }
            } while(lobbyGathering);
        }
        System.out.println("END LOBBY");
    }

    private void checkConnections() {
        int i = 0;
        while(i < model.getNumPlayers()) {
            if(!model.checkConnection(i) || !view.checkConnection(i)) {
                //take into account that the player has quitted
                if(lobbyGathering) {
                    model.removeClient(i);
                    view.removeClient(i);
                    playerActions.remove(i);
                }
                else {
                    model.removePlayer(i);
                    i++;
                }
            }
            else {
                i++;
            }
        }
    }

    private boolean emptyPreLobby() {
        Iterator<ClientInfo> iterator = clientGatherer.iterator();
        while(iterator.hasNext()) {
            if(iterator.next().getPlayerAction().isUpdated()) {
                return false;
            }
        }
        return true;
    }

    private void gameSetup() {
        int numPlayer = model.getNumPlayers();
        PatternCard[] patternCards = new PatternCard[numPlayer * 2];
        PubObjCard[] pubObjCards = new PubObjCard[3];
        int numPCs = 8;
        int numPubOcs = 8; // TODO: ?? from file
        ArrayList<Integer> pcIds = new ArrayList<>();
        ArrayList<Integer> pubOCIds = new ArrayList<>();
        for(int i = 0; i < numPCs; i++) {
            pcIds.add(i);
        }
        for(int i = 0; i < numPubOcs; i++) {
            pubOCIds.add(i);
        }
        Random random = new Random();
        //TODO: for(int i = 0; i < numPlayer * 2; i++) {
        for(int i = 0; i < 4; i++) {
            int num = random.nextInt(pcIds.size());
            int id = pcIds.remove(num);
            System.out.println("PC:"+ id);
            patternCards[i] = resourceLoader.loadPC(id);
        }
        for(int i = 0; i < 3; i++) {
            int num = random.nextInt(pubOCIds.size());
            int id = pubOCIds.remove(num);
            pubObjCards[i] = resourceLoader.loadPubOC(id);
        }
        model.setPatternCards(patternCards);
        model.setPubOCs(pubObjCards);
    }

    private void patternCardsSelection() {
        ArrayList<PatternCard> patternCards = new ArrayList<>(Arrays.asList(model.getPatternCards()));
        for(int i = 0; i < model.getNumPlayers(); i++) {
            ViewInterface playerView = view.getView(i);
            int offset = 2 * i;
            ArrayList<PatternCard> playerPCs = new ArrayList<>(patternCards.subList(offset, offset + 2));
            try {
                playerView.patternCardGenerator(playerPCs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean rangeCheck(int[] array){
        if(array.length == 2){
            return array[0] >= 0 && array[0] <= 3 && array[1] >= 0 && array[1] <= 4;
        }
        else return false;
    }

    private boolean nullCheck(PlayerAction pa, int[] array){
        if(array.length == 2){
            return model.getPlayer(playerActions.indexOf(pa)).getWFPosition(array[0], array[1]) == null;
        }
        else return false;
    }

    private boolean validAction(PlayerAction pa) {

        if(pa.getConnectionType() != null) {
            switch(pa.getConnectionType()) {
                case "socket":
                case "rmi":
                    return true;
                default:
                    return false;
            }
        }

        if (!(pa.getIdToolCard()>=0 && pa.getIdToolCard()<=12)){
            return false;
        }

        for(int dieFace : pa.getNewDieValue()){
            if(!(dieFace >= 1 && dieFace <= 6)){
                return false;
            }
        }

        //Checking for the position in the Draft Pool
        for(int value : pa.getPosDPDie()){
            if(!(value >= 0 && model.getDraftPoolDie(value) != null)){
                return false;
            }
        }

        for(int[] posRT : pa.getPosRTDie()){
            if(posRT[0] >=0 && posRT[0] < model.getRound()
                    && posRT[1] >=0 && posRT[1] < model.getNumRTDiceRound(posRT[0])){
                if(model.getRoundTrackDie(posRT[0],posRT[1]) != null){
                    return false;
                }
            }
            else return false;
        }

        //verify if the places in the WF are empty.
        for(int[] array: pa.getPlaceDPDie()){
            if(!(rangeCheck(array) && nullCheck(pa,array))){
                return false;
            }
        }

        //control if a designed position to be deleted is full.
        for(int[] array: pa.getPlaceWFDie()){
            if(!rangeCheck(array)){
                return false;
            }
        }

        //Afterward the verification of the future placement, this code portion verifies if parameters are legal.
        for(int[] array: pa.getPlaceNewWFDie()){
            if(!rangeCheck(array)){
                return false;
            }
        }

        if ((pa.getIdToolCard() > 0)
                && model.getToolCard(pa.getIdToolCard()).validAction(model, model.getPlayer(playerActions.indexOf(pa)).getWF(), pa)){
            return true;
        }
        else if(!pa.getPosDPDie().isEmpty()) {
            if(pa.getPosDPDie().get(0) >= 0){
                return (rangeCheck(pa.getPlaceDPDie().get(0))
                        && model.getPlayer(playerActions.indexOf(pa)).getWF().checkRestrictions(model.getDraftPoolDie(pa.getPosDPDie().get(0)),pa.getPlaceDPDie().get(0)[0],pa.getPlaceDPDie().get(0)[1]));
            }
            else return false;
        }
        return true;
    }

    private void performAction(PlayerAction pa) {
        if(!lobbyGathering && !model.isStarted()) {
            Player player = model.getPlayer(playerActions.indexOf(pa));
            int index = pa.getPatternCard()/2 + 2*playerActions.indexOf(pa);
            boolean wcFace = (pa.getPatternCard()%2 == 0);
            PatternCard pc = model.getPatternCards()[index];
            player.setWinFrame(new WindowFrame(pc,wcFace));
        }
        //TODO VERIFY ALL PLAYER CHOSE A PATTERNCARD
        if(pa.getConnectionType() != null) {
            int playerIndex = playerActions.indexOf(pa);
            model.removePlayer(playerIndex);
        }
        if(model.isStarted()) {
            if((pa.getIdToolCard() == 0)) {
                // regular turn without choosing a tool card
                Die die = model.removeDraftPoolDie(pa.getPosDPDie().get(0));
                int[] place = pa.getPlaceDPDie().get(0);
                int playerIndex = playerActions.indexOf(pa);
                model.placeWFDie(playerIndex, die, place[0], place[1]);
            }
            else {
                int playerIndex = playerActions.indexOf(pa);
                model.performToolCard(playerIndex, pa);
            }
        }
    }

    public static void main(String[] args) {
        Model model = Model.instance();
        VirtualView virtualView = new VirtualView();
        ServerController serverController = new ServerController(model, virtualView);
        model.addObserver(virtualView);
        try {
            serverController.gatherPlayers();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        serverController.gameSetup();
        serverController.patternCardsSelection();
        try {
            serverController.listenPlayerActions();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
