package model;

import java.io.Serializable;
import java.util.ArrayList;

import static util.Configs.BOARD_DIMENSION;

/**
 *
 */
public class Board implements Serializable {
    private Square[][] squares;
    private ArrayList<Ship> ships;
    private boolean ownBoard;

    public Board(boolean ownBoard) {
        this.ownBoard = ownBoard;
        squares = new Square[BOARD_DIMENSION][BOARD_DIMENSION];
        for (int i = 0; i < BOARD_DIMENSION; i++) {
            for (int j = 0; j < BOARD_DIMENSION; j++) {
                squares[i][j] = new Square(i, j, ownBoard);
            }
        }

        ships = new ArrayList<>();
        ships.add(new Ship(Ship.Type.AIRCRAFT_CARRIER));
        ships.add(new Ship(Ship.Type.BATTLESHIP));
        ships.add(new Ship(Ship.Type.DESTROYER));
        ships.add(new Ship(Ship.Type.PATROL_BOAT));
        ships.add(new Ship(Ship.Type.SUBMARINE));
    }

    public boolean gameOver() {
        for (Ship ship : ships) {
            if (!ship.isSunk())
                return false;
        }
        return true;
    }

    public ArrayList<Ship> getShips() {
        return ships;
    }

    public Square getSquare(int x, int y) {
        return squares[x][y];
    }

    public boolean isOwnBoard() {
        return (ownBoard);
    }
}
