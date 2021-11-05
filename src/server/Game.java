package server;


import model.game.Ship;
import model.game.Square;
import model.messages.MoveMessage;
import model.messages.MoveResponseMessage;
import model.messages.NotificationMessage;
import util.NotificationCode;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static util.Configs.*;
import static util.NotificationCode.*;

public class Game {

    private Player player1;
    private Player player2;
    private Player turn;

    private Timer placementTimer;
    private Timer turnTimer;

    private boolean gameStarted;

    //Khởi tạo 1 game, thông báo cho player tên đối phương, bộ đếm thời gian bắt đầu
    public Game(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        player1.setGame(this);
        player2.setGame(this);
        player1.writeNotification(OPPONENTS_NAME, player2.getPlayerName());
        player2.writeNotification(OPPONENTS_NAME, player1.getPlayerName());
        NotificationMessage placeShipsMessage = new NotificationMessage(PLACE_SHIPS);

        player1.writeObject(placeShipsMessage);
        System.out.println(">> " + player1.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.PLACE_SHIPS);
        player2.writeObject(placeShipsMessage);
        System.out.println(">> " + player2.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.PLACE_SHIPS);

        placementTimer = new Timer();
        placementTimer.schedule(new PlacementTimerTask(), PLACEMENT_TIMEOUT);
    }

    //get người chơi còn lại, không phải player đang được chỉ định
    public Player getOpponent(Player self) {
        if (player1 == self) {
            return player2;
        }
        return player1;
    }

    //set game cho cả 2 player về null
    public void killGame() {
        player1.setGame(null);
        player2.setGame(null);
    }

    // Set turn cho player được truyền vào
    // Set 1 new turnTime
    // Gửi thông báo tương ứng cho 2 bên
    public synchronized void setTurn(Player player) {
        turn = player;
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        turnTimer = new Timer();
        turnTimer.schedule(new TurnTimerTask(), TURN_TIMEOUT);
        turn.writeNotification(YOUR_TURN);
        System.out.println(">> " + turn.socket.getRemoteSocketAddress().toString() + " " + YOUR_TURN);
        getOpponent(turn).writeNotification(OPPONENTS_TURN);
        System.out.println(">> " + getOpponent(turn).socket.getRemoteSocketAddress().toString() + " " + OPPONENTS_TURN);
    }

    //neu 2 player co borad hợp lệ thì start game
    public void checkBoards() {
        if (player1.getBoard() != null && player2.getBoard() != null) {
            placementTimer.cancel();
            startGame();
        }
    }

    //random turn
    private void startGame() {
        gameStarted = true;
        if (new Random().nextInt(2) == 0) {
            setTurn(player1);
        } else {
            setTurn(player2);
        }
    }

    // bắn tàu
    public synchronized void applyMove(MoveMessage move, Player player) {
        int x = move.getX();
        int y = move.getY();
        int max = BOARD_DIMENSION;

        System.out.println("<< " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.SHOT + " " + x + " " + y);

        if (player != turn) {
            System.out.println(">> " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.NOT_YOUR_TURN);
            player.writeNotification(NotificationCode.NOT_YOUR_TURN);
            return;
        }

        if (x < 0 || x >= max || y < 0 || y >= max) {
            System.out.println(">> " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.INVALID_MOVE);
            player.writeNotification(NotificationCode.INVALID_MOVE);
        } else {
            Player opponent = getOpponent(player);
            Square square = opponent.getBoard().getSquare(x, y);
            if (square.isGuessed()) {
                System.out.println(">> " + player.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.REPEATED_MOVE);
                player.writeNotification(NotificationCode.REPEATED_MOVE);
                return;
            }

            boolean hit = square.guess();
            Ship ship = square.getShip();
            MoveResponseMessage response;
            if (ship != null && ship.isSunk()) {
                response = new MoveResponseMessage(x, y, ship, true, false);
            } else {
                response = new MoveResponseMessage(x, y, null, hit, false);
            }
            player.writeObject(response);
            response.setOwnBoard(true);
            opponent.writeObject(response);

            System.out.println(">> " + opponent.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.SHOT + " " + x + " " + y);

            if (opponent.getBoard().gameOver()) {
                System.out.println(">> " + turn.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.GAME_WIN);
                turn.writeNotification(NotificationCode.GAME_WIN);

                System.out.println(">> " + opponent.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.GAME_LOSE);
                opponent.writeNotification(NotificationCode.GAME_LOSE);
                turn = null;
            } else if (hit) {
                setTurn(player); // player gets another go if hit
            } else {
                setTurn(getOpponent(player));
            }
        }
    }

    private class PlacementTimerTask extends TimerTask {
        @Override
        public void run() {
            if (player1.getBoard() == null & player2.getBoard() == null) {
                NotificationMessage draw = new NotificationMessage(NotificationCode.TIMEOUT_DRAW);
                player1.writeObject(draw);
                player2.writeObject(draw);
                System.out.println(">> " + player1.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.TIMEOUT_DRAW);
                System.out.println(">> " + player2.socket.getRemoteSocketAddress().toString() + " " + NotificationCode.TIMEOUT_DRAW);
                killGame();
            } else if (player1.getBoard() == null) {
                // Player1 failed to place ships in time
                player1.writeNotification(NotificationCode.TIMEOUT_LOSE);
                player2.writeNotification(NotificationCode.TIMEOUT_WIN);
                killGame();
            } else if (player2.getBoard() == null) {
                // Player2 failed to place ships in time
                player1.writeNotification(NotificationCode.TIMEOUT_WIN);
                player2.writeNotification(NotificationCode.TIMEOUT_LOSE);
                killGame();
            }
        }
    }

    private class TurnTimerTask extends TimerTask {
        @Override
        public void run() {
            if (turn != null) {
                turn.writeNotification(NotificationCode.TIMEOUT_LOSE);
                getOpponent(turn).writeNotification(NotificationCode.TIMEOUT_WIN);
                killGame();
            }
        }
    }

}
