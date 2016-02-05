package ca.alexcomeau.texmobile.game;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;


public class Board implements Parcelable {
    private Block.Shape[][] stack;

    public Board()
    {
        // [Y][X] -- makes things a lot easier
        stack = new Block.Shape[22][10];
    }

    // Checks whether the block is currently in a legal position.
    public boolean checkBlock(Block block)
    {
        Point[] coords = block.getAbsoluteCoordinates();

        for(Point c : coords)
        {
            if(c.y < 0 || c.x < 0 || c.x >= stack[0].length)
                return false;
            if (stack[c.y][c.x] != null)
                return false;
        }
        return true;
    }

    // Returns whether there is room for the given piece to move left
    public boolean checkLeft(Block block)
    {
        // Check if it would fit to the left, then move it back.
        block.moveLeft();
        boolean result = checkBlock(block);
        block.moveRight();
        return result;
    }

    // Returns whether there is room for the given piece to move right
    public boolean checkRight(Block block)
    {
        // Check if it would fit to the right, then move it back.
        block.moveRight();
        boolean result = checkBlock(block);
        block.moveLeft();
        return result;
    }

    // Returns whether there is room for the given piece to move down
    public boolean checkDown(Block block)
    {
        // Check if it would fit below, then move it back.
        block.moveDown();
        boolean result = checkBlock(block);
        block.moveUp();
        return result;
    }

    // Returns whether there is room for the given piece to move down
    public boolean checkRotateRight(Block block)
    {
        // Check if it would fit rotated, then put it back.
        block.rotateRight();
        boolean result = checkBlock(block);
        block.rotateLeft();
        return result;
    }

    // Returns whether there is room for the given piece to move down
    public boolean checkRotateLeft(Block block)
    {
        // Check if it would fit rotated, then put it back.
        block.rotateLeft();
        boolean result = checkBlock(block);
        block.rotateRight();
        return result;
    }

    // Returns whether the specified line is filled, and thus can be cleared.
    public boolean checkLine(int line)
    {
        for(Block.Shape col : stack[line])
        {
            // Check each cell in the specified row for null values
            if (col == null)
                return false;
        }
        return true;
    }

    // Clears all the blocks from the specified line.
    public void clearLine(int line)
    {
        // Lower all the above lines by one
        for(int k = line; k < 21; k++)
            System.arraycopy(stack[k + 1], 0, stack[k], 0, 10);

        // The top row will always be all black, blocks cannot be placed there
    }

    // Saves a block to the stack.
    public void lockBlock(Block block)
    {
        // Color in the places the block is over
        for(Point c : block.getAbsoluteCoordinates())
            stack[c.y][c.x] = block.getShape();
    }

    public Block.Shape[][] getStack() { return stack; }

    public boolean equals(Board other)
    {
        // If any of the rows aren't the same, the stacks aren't the same
        for(int i = 0; i < stack.length; i++)
        {
            if(!Arrays.equals(stack[i], other.getStack()[i]))
                return false;
        }
        return true;
    }

    // ===== Parcelable Stuff ============================
    protected Board(Parcel in) {
        stack = (Block.Shape[][]) in.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(stack);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Board> CREATOR = new Parcelable.Creator<Board>() {
        @Override
        public Board createFromParcel(Parcel in) {
            return new Board(in);
        }

        @Override
        public Board[] newArray(int size) {
            return new Board[size];
        }
    };
}
