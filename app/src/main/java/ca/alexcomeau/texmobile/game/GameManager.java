package ca.alexcomeau.texmobile.game;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GameManager implements Parcelable {
    private Block currentBlock;
    private Block nextBlock;
    private Block lastBlock;
    private Board gameBoard;
    private int level;
    private int score;
    private int lockWait;
    private int droppedLines;
    private int combo;
    private int spawnWait;
    private int fallWait;
    private int autoShiftWait;
    private int lineClearWait;
    private int elapsedFrames;
    private int soundEffectToPlay;
    private List<String> lastInput;
    private LinkedList<Block.Shape> history;
    private boolean grandmasterValid;
    private boolean check1;
    private boolean check2;
    private boolean check3;
    private boolean pieceRedraw;
    private boolean stackRedraw;
    private boolean spawned;
    private Boolean gameOver;

    // Pieces drop every gravity frames, if gravity > 0.
    private int gravity;
    // Pieces drop superGravity rows per frame, if gravity == 0.
    private int superGravity;
    // When the game ends
    private int maxLevel;

    // Blocks lock in place LOCK_DELAY frames after touching the stack. Gives time to rotate and move, especially in superGravity 20.
    private final int LOCK_DELAY = 15;
    // Where pieces spawn
    private final Point START = new Point(3,17);
    // Number of different block shapes
    private final int SHAPES_COUNT = Block.Shape.values().length;
    // Pieces are spawned SPAWN_DELAY frames after a piece is locked.
    private final int SPAWN_DELAY = 15;
    // Frames to wait before allowing consecutive duplicate inputs. Too low and they get accidentally doubled
    private final int AUTO_SHIFT_DELAY = 7;
    // Frames to wait after a line is cleared before doing anything else. Rewards multiline clears.
    private final int LINE_CLEAR_DELAY = 21;
    // Number of times to attempt to generate a piece that's not in history
    private final int GENERATION_TRIES = 4;

    public GameManager(){ }

    // Start the game
    public void start(int levelStart, int levelEnd)
    {
        // Initialize
        gameBoard = new Board();
        score = 0;
        level = 0;
        gravity = 0;
        superGravity = 0;
        maxLevel = levelEnd;
        addLevel(levelStart);
        combo = 1;
        gameOver = null;
        pieceRedraw = true;
        stackRedraw = false;
        elapsedFrames = 0;
        spawnWait = 0;
        lockWait = 0;
        autoShiftWait = 0;
        fallWait = 0;
        soundEffectToPlay = -1;
        lineClearWait = LINE_CLEAR_DELAY;
        lastInput = new ArrayList<>();

        // Start the history full of Zs.
        history = new LinkedList<>();
        history.add(Block.Shape.Z);
        history.add(Block.Shape.Z);
        history.add(Block.Shape.Z);
        history.add(Block.Shape.Z);

        // Don't generate an O, S, or Z as the first piece
        currentBlock = generateNewBlock((int)(Math.random() * 4));
        nextBlock = generateNewBlock();

        // If they're doing a full game they can attain grandmaster rank
        grandmasterValid = (maxLevel == 999 && levelStart == 0);
        check1 = grandmasterValid;
        check2 = grandmasterValid;
        check3 = grandmasterValid;
    }

    // Move ahead a frame
    public void advanceFrame(List<String> in)
    {
        spawned = false;
        elapsedFrames++;

        if(lineClearWait < LINE_CLEAR_DELAY)
            lineClearWait ++;
        else
        {
            // Copy it over so we can mess with it without affecting the original
            List<String> input = new ArrayList<>();
            input.addAll(in);

            if (lastInput.contains("rotateRight") || lastInput.contains("rotateLeft"))
            {
                // Rotating every frame is not desired
                input.remove("rotateRight");
                input.remove("rotateLeft");
            }

            // Check if any the received input is the same as last frame
            if(!Collections.disjoint(input, lastInput))
            {
                // If so, wait some frames before accepting it again to avoid unintentional doubled inputs
                // This needs to be checked even if there's no piece so they can "charge" fast movement
                if (autoShiftWait < AUTO_SHIFT_DELAY && !input.isEmpty())
                {
                    // Remove the movements -- rotations were handled previously
                    input.remove("left");
                    input.remove("right");
                    input.remove("down");
                    autoShiftWait++;
                }

            }
            else
                autoShiftWait = 0;

            // Update the last input, using the original because we might have removed some
            lastInput.clear();
            lastInput.addAll(in);

            if(currentBlock == null)
            {
                if(spawnWait++ >= SPAWN_DELAY)
                {
                    // Bring in the next block and generate a new next
                    currentBlock = nextBlock;
                    nextBlock = generateNewBlock();
                    pieceRedraw = true;

                    spawned = true;

                    spawnWait = 0;
                    fallWait = 0;
                    droppedLines = 1;

                    // A new block appearing increases the level by one, unless the level ends in 99 or is the second last
                    if(!((level + 1) % 100 == 0 || level == maxLevel - 1))
                        addLevel(1);
                }
            }

            if(currentBlock != null)
            {
                if(spawned)
                {
                    // Only allow rotations on spawn
                    if(in.contains("rotateLeft"))
                        handleInput("rotateLeft");
                    if(in.contains("rotateRight"))
                        handleInput("rotateRight");
                }
                else
                    for(String s : input)
                        if(s != null) handleInput(s);

                // If the new block isn't in a valid location upon spawning (and rotating), the game is lost
                if(spawned)
                    if(!gameBoard.checkBlock(currentBlock))
                        gameOver = false;

                // Check if the block is currently in a state of falling
                if(gameBoard.checkDown(currentBlock))
                {
                    lockWait = 0;
                    // Move the block down if enough time has passed
                    if(gravity > 0)
                    {
                        fallWait++;
                        if(fallWait >= gravity)
                        {
                            fallWait = 0;
                            currentBlock.moveDown();
                            pieceRedraw = true;
                        }
                    }
                    else
                    {
                        for(int i = 0; i < superGravity; i++)
                            if(gameBoard.checkDown(currentBlock))
                            {
                                currentBlock.moveDown();
                                pieceRedraw = true;
                            }
                    }
                }
                else
                {
                    lockWait++;
                    // Check if the block needs to be locked
                    if(lockWait >= LOCK_DELAY)
                    {
                        gameBoard.lockBlock(currentBlock);
                        lockWait = 0;
                        soundEffectToPlay = 0;
                        // Check if locking that piece caused any lines to be cleared
                        checkClears();
                        lastBlock = currentBlock;
                        currentBlock = null;
                    }
                }
            }
        }
    }

    // ===== Input handling methods ==========================================
    private void handleInput(String input)
    {
        switch(input)
        {
            case "left":
                moveLeft();
                break;
            case "right":
                moveRight();
                break;
            case "rotateLeft":
                rotateLeft();
                break;
            case "rotateRight":
                rotateRight();
                break;
            case "down":
            {
                // Make the piece fall or lock immediately
                lockWait = LOCK_DELAY;
                fallWait = gravity;
                droppedLines++;
                break;
            }
            default:
                break;
        }
    }

    private void moveLeft()
    {
        if(gameBoard.checkLeft(currentBlock))
        {
            currentBlock.moveLeft();
            pieceRedraw = true;
        }
    }

    private void moveRight() {
        if (gameBoard.checkRight(currentBlock))
        {
            currentBlock.moveRight();
            pieceRedraw = true;
        }
    }

    private void rotateLeft()
    {
        if(gameBoard.checkRotateLeft(currentBlock))
        {
            currentBlock.rotateLeft();
            pieceRedraw = true;
        }
        else if (currentBlock.getShape() != Block.Shape.I) // I blocks can't wall kick
        {
            // See if the rotation would be valid if the block was tapped to the side (wall kick)
            currentBlock.moveRight();

            if(gameBoard.checkRotateLeft(currentBlock))
            {
                currentBlock.rotateLeft();
                pieceRedraw = true;
            }

            // Undo the move right if it still didn't work
            else
                currentBlock.moveLeft();
        }
    }

    private void rotateRight()
    {
        if(gameBoard.checkRotateRight(currentBlock))
        {
            currentBlock.rotateRight();
            pieceRedraw = true;
        }
        else if (currentBlock.getShape() != Block.Shape.I) // I blocks can't wall kick
        {
            // See if the rotation would be valid if the block was tapped to the side (wall kick)
            currentBlock.moveLeft();

            if(gameBoard.checkRotateRight(currentBlock))
            {
                currentBlock.rotateRight();
                pieceRedraw = true;
            }
                // Undo the move left if it still didn't work
            else
                currentBlock.moveRight();
        }
    }
    // ===== End input handling =============================================

    // Checks for clears, clears them if so, and adds to the score and level
    private void checkClears()
    {
        int linesCleared = 0;

        // This only works because the shape rotations are in descending order. Check each row once
        ArrayList<Integer> toCheck = new ArrayList<>();
        for(Point c : currentBlock.getAbsoluteCoordinates())
            if (!toCheck.contains(c.y))
            {
                toCheck.add(c.y);
                if (gameBoard.checkLine(c.y))
                {
                    gameBoard.clearLine(c.y);
                    linesCleared++;
                }
            }

        if(linesCleared > 0)
        {
            // Multiplier for clearing the whole screen
            int bravo = gameBoard.equals(new Board()) ? 4 : 1;

            // Tetris: The Grand Master scoring method
            score += (Math.ceil((level + linesCleared) / 4) + droppedLines)
                    * linesCleared * ((linesCleared * 2) - 1)
                    * combo * bravo;

            // Add to the combo
            combo += (linesCleared * 2) - 2;

            addLevel(linesCleared);

            // The game ends once the max level is reached.
            if(level >= maxLevel)
            {
                level = maxLevel;
                if(check3)
                {
                    // Final check. Score >= 126000, Time <= 13m30s. 1 frame per 34 ms.
                    if(score < 126000 && elapsedFrames * 34 > 810000)
                        grandmasterValid = false;
                    check3 = false;
                }
                gameOver = true;
            }

            stackRedraw = true;
            soundEffectToPlay = 1;
            lineClearWait = 0;
        }
        else
            combo = 1;
    }

    // Generates a block of a random type.
    private Block generateNewBlock(int i)
    {
        Block.Shape result = Block.Shape.values()[i];

        // Take out the oldest element of history and add in this one
        history.remove();
        history.add(result);

        return new Block(START, result);
    }

    private Block generateNewBlock()
    {
        int i = (int)(Math.random() * SHAPES_COUNT);
        int j = 0;

        // Generate a new number until there's one that's not in the history, or the limit is reached
        while(history.contains(Block.Shape.values()[i]) && j < GENERATION_TRIES)
        {
            i = (int)(Math.random() * SHAPES_COUNT);
            j++;
        }

        return generateNewBlock(i);
    }

    private void addLevel(int toAdd)
    {
        level += toAdd;

        // Gravity changes depending on level
        if(level < 30)
            gravity = 32;
        else if(level < 35)
            gravity = 21;
        else if(level < 40)
            gravity = 16;
        else if(level < 50)
            gravity = 13;
        else if(level < 60)
            gravity = 10;
        else if(level < 70)
            gravity = 8;
        else if(level < 80)
            gravity = 4;
        else if(level < 140)
            gravity = 3;
        else if(level < 170)
            gravity = 2;
        else if(level < 200)
            gravity = 32;
        else if(level < 220)
            gravity = 4;
        else if(level < 230)
            gravity = 2;
        else if(level < 251)
            gravity = 1;
        else if(level < 300)
        {
            // Here, the pieces start dropping multiple rows per frame.
            gravity = 0;
            superGravity = 1;

            // This is one of the checkpoints for grandmaster rank. Score >= 12000, Time <= 4m15s
            if(check1)
            {
                if (score < 12000 || elapsedFrames * 34 > 255000)
                {
                    grandmasterValid = false;
                    // No need to do the other checks if one fails.
                    check2 = false;
                    check3 = false;
                }
                check1 = false;
            }
        }
        else if(level < 330)
            superGravity = 2;
        else if(level < 360)
            superGravity = 3;
        else if(level < 400)
            superGravity = 4;
        else if(level < 420)
            superGravity = 5;
        else if(level < 450)
            superGravity = 4;
        else if(level < 500)
            superGravity = 3;
        else if(level < 999)
        {
            superGravity = 20;

            // Another checkpoint. Score >= 40000, Time <= 7m30s
            if(check2)
            {
                if (score < 40000 || elapsedFrames * 34 > 450000)
                {
                    grandmasterValid = false;
                    // No need to do the other checks if one fails.
                    check3 = false;
                }
                check2 = false;
            }
        }
    }

    public String getGrade()
    {
        if(score < 400)
            return "9";
        else if(score < 800)
            return "8";
        else if(score < 1400)
            return "7";
        else if(score < 2000)
            return "6";
        else if(score < 3500)
            return "5";
        else if(score < 5500)
            return "4";
        else if(score < 8000)
            return "3";
        else if(score < 12000)
            return "2";
        else if(score < 16000)
            return "1";
        else if(score < 22000)
            return "S1";
        else if(score < 30000)
            return "S2";
        else if(score < 40000)
            return "S3";
        else if(score < 52000)
            return "S4";
        else if(score < 66000)
            return "S5";
        else if(score < 82000)
            return "S6";
        else if(score < 100000)
            return "S7";
        else if(score < 120000)
            return "S8";
        else if(grandmasterValid && score >= 126000)
            return "GM";
        else
            return "S9";
    }

    public Block.Shape[][] getStack() { return gameBoard.getStack(); }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    public int getScore() { return score; }
    public int getFrames() { return elapsedFrames; }
    public int getSoundEffectToPlay() { return soundEffectToPlay; }
    public Boolean getGameOver() { return gameOver; }
    public Block getCurrentBlock() { return currentBlock; }
    public Block getNextBlock() { return nextBlock; }
    public boolean getStackRedraw() { return stackRedraw; }
    public boolean getPieceRedraw() { return pieceRedraw; }
    public Block getLastBlock() { return lastBlock; }
    public void clearLastBlock() { lastBlock = null; }
    public void clearSoundEffect() { soundEffectToPlay = -1; }
    public void clearRedraw() { pieceRedraw = false; stackRedraw = false;}

    // ===== Parcelable Stuff ============================================
    protected GameManager(Parcel in) {
        currentBlock = in.readParcelable(Block.class.getClassLoader());
        nextBlock = in.readParcelable(Block.class.getClassLoader());
        gameBoard = in.readParcelable(Board.class.getClassLoader());
        level = in.readInt();
        score = in.readInt();
        lockWait = in.readInt();
        droppedLines = in.readInt();
        combo = in.readInt();
        spawnWait = in.readInt();
        fallWait = in.readInt();
        autoShiftWait = in.readInt();
        lineClearWait = in.readInt();
        elapsedFrames = in.readInt();
        if (in.readByte() == 0x01) {
            history = new LinkedList<>();
            in.readList(history, Integer.class.getClassLoader());
        } else {
            history = null;
        }
        grandmasterValid = in.readByte() != 0x00;
        check1 = in.readByte() != 0x00;
        check2 = in.readByte() != 0x00;
        check3 = in.readByte() != 0x00;
        pieceRedraw = true;
        stackRedraw = true;
        byte gameOverVal = in.readByte();
        gameOver = gameOverVal == 0x02 ? null : gameOverVal != 0x00;
        gravity = in.readInt();
        superGravity = in.readInt();
        maxLevel = in.readInt();
        soundEffectToPlay = -1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(currentBlock, 0);
        dest.writeParcelable(nextBlock, 0);
        dest.writeParcelable(gameBoard, 0);
        dest.writeInt(level);
        dest.writeInt(score);
        dest.writeInt(lockWait);
        dest.writeInt(droppedLines);
        dest.writeInt(combo);
        dest.writeInt(spawnWait);
        dest.writeInt(fallWait);
        dest.writeInt(autoShiftWait);
        dest.writeInt(lineClearWait);
        dest.writeInt(elapsedFrames);
        if (history == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(history);
        }
        dest.writeByte((byte) (grandmasterValid ? 0x01 : 0x00));
        dest.writeByte((byte) (check1 ? 0x01 : 0x00));
        dest.writeByte((byte) (check2 ? 0x01 : 0x00));
        dest.writeByte((byte) (check3 ? 0x01 : 0x00));
        if (gameOver == null) {
            dest.writeByte((byte) (0x02));
        } else {
            dest.writeByte((byte) (gameOver ? 0x01 : 0x00));
        }
        dest.writeInt(gravity);
        dest.writeInt(superGravity);
        dest.writeInt(maxLevel);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<GameManager> CREATOR = new Parcelable.Creator<GameManager>() {
        @Override
        public GameManager createFromParcel(Parcel in) {
            return new GameManager(in);
        }

        @Override
        public GameManager[] newArray(int size) {
            return new GameManager[size];
        }
    };
}
