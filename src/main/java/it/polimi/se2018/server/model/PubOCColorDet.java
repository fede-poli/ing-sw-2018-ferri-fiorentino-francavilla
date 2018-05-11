package it.polimi.se2018.server.model;

import java.util.ArrayList;

public class PubOCColorDet extends PubObjCard {
    //attributes
    private boolean rows;
    private boolean col;
    private boolean diagonals;
    private boolean set;
    private ArrayList<Integer> check = new ArrayList<Integer>(0);
    //methods
    public PubOCColorDet(String desc) {
        super(desc);
    }

    private void updateCheck(ArrayList<Integer> localCheck, int row , int col, WindowFrame wf){
        //UpdateCheck is used to fill the "Check" vector with how many times each color appears on a line
        if(wf.getDie(row,col).getColor() == Color.BLUE){
            localCheck.set(0,localCheck.get(0) + 1);
        }
        else if(wf.getDie(row,col).getColor() == Color.YELLOW){
            localCheck.set(1,localCheck.get(1) + 1);
        }
        else if(wf.getDie(row,col).getColor() == Color.RED){
            localCheck.set(2,localCheck.get(2) + 1);
        }
        else if(wf.getDie(row,col).getColor() == Color.GREEN){
            localCheck.set(3,localCheck.get(3) + 1);
        }
        else if(wf.getDie(row,col).getColor() == Color.PURPLE){
            localCheck.set(4,localCheck.get(4) + 1);
        }
    }


    public int calculateScore(WindowFrame wf) {
        //calculateScore is used to calculate the current score of the player
        int score = 0;

        if (rows){
            //IF true it will count on how many rows each dice color is different from the others
            for (int i = 0; i<5; i++){
                //I added 5 elements initialized to 0 to the arraylist
                check.add(0);
            }
            for (int i = 0; i< 4; i++){
                for(int j = 0; j<5; j++){
                    //we run the method to see how many times a certain color appears on a single row
                    updateCheck(check,i,j,wf);
                }
                if(check.get(0) == 1 && check.get(1) == 1 && check.get(2) == 1 && check.get(3) == 1 && check.get(4) == 1){
                    //if in one row each color came out once then we can add 6 points to the score
                    score = score + 6;
                }
                for(int l = 0; l<5;l++){
                    //we need to reset the elements of the ArrayList to 0
                    check.set(l, 0);
                }
            }
                //We need to deconstruct the ArrayList
                //TODO CHECK CHE QUESTO ELIMINA L ARRAY SE NON LO FA USARE REMOVE ALL O USARE UN FOR
                check.clear();
        }
        if (col){
            //IF true it will count on how many columns each dice color is different from the others
            for (int i = 0; i<4; i++){
                //I added 4 elements initialized to 0 to the arraylist
                check.add(0);
            }
            for (int i = 0; i< 5; i++){
                for(int j = 0; j<4; j++){
                    //we run the method to see how many times a certain color appears on a single column
                    updateCheck(check,j,i,wf);
                }
                if(check.get(0) <= 1 && check.get(1) <= 1 && check.get(2) <= 1 &&
                        check.get(3) <= 1 && check.get(4) <= 1
                        &&(check.get(0) + check.get(1) + check.get(2) + check.get(3) +  check.get(4)) == 4){
                    //if in one column each color came out at most once but none of the spaces are empty
                    // then we can add 5 points to the score
                    score = score + 5;
                }
                    //We need to deconstruct the ArrayList
                    check.clear();

        }
        if(diagonals){
            //TODO diagonals
        }
        if(set){
            for (int i = 0; i<5; i++){
                //I added 5 elements initialized to 0 to the arraylist ( as many elements as the colors)
                check.add(0);
            }
            for (int i = 0; i<5; i++){
                for (int l = 0;l<4;l++){
                    //fill check with all of the elements inside the window frame
                    updateCheck(check,l,i,wf);
                }
            }
            //i look for the color that appeared the least(because it is going to be equal to the number of sets of colors)
            int min = check.get(0);
            for (int j=1; j<5;j++){
                if(check.get(j)<min){
                    min = check.get(j);
                }
                //i update the score it is equal to the number of sets times 4
                score = score + min*4;
                check.clear();
            }

        }
        }
        return score;
    }
}
