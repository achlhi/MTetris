package ca.alexcomeau.texmobile.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Hashtable;

import ca.alexcomeau.texmobile.R;
import ca.alexcomeau.texmobile.activities.GameActivity;

public class GameView extends SurfaceView implements SurfaceHolder.Callback
{
    private GameThread thread;
    private GameManager game;
    private Context context;
    private GameActivity activity;
    private int rectWidth;
    private boolean gameStarted;
    private Hashtable<Block.Shape, NinePatchDrawable> htShapes;
    private Bitmap stackState;

    public GameView(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        context = ctx;
        getHolder().addCallback(this);
        setFocusable(true);
        gameStarted = false;
        // Initialize
        stackState = Bitmap.createBitmap(1,1, Bitmap.Config.RGB_565);
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec)
    {
        // This override is so the view will fill all available space, while also maintaining its aspect ratio.
        int heightMode = MeasureSpec.getMode(heightSpec);
        int widthMode = MeasureSpec.getMode(widthSpec);
        int widthSize = MeasureSpec.getSize(widthSpec);
        int heightSize = MeasureSpec.getSize(heightSpec);
        int width, height;

        if (widthMode == MeasureSpec.EXACTLY)
            width = widthSize;
        else if (widthMode == MeasureSpec.AT_MOST)
        {
            width = Math.min(heightSize / 2, widthSize);
            // Trim excess. There's 10 columns, so it needs to be divisible by 10
            width = width - (width % 10);
        }
        else
            width = Integer.MAX_VALUE;

        if (heightMode == MeasureSpec.EXACTLY)
            height = heightSize;
        else if (heightMode == MeasureSpec.AT_MOST)
        {
            height = Math.min(width * 2, heightSize);
            // Trim excess. There's 20 rows, so it needs to be divisible by 20
            height = height - (height % 20);
        }
        else
            height = Integer.MAX_VALUE;

        this.setMeasuredDimension(width, height);
    }

    public void setupGame(int start, int end, GameActivity activity)
    {
        game = new GameManager();
        game.start(start, end);

        setupGame(game, activity);
    }

    public void setupGame(GameManager game, GameActivity activity)
    {
        this.game = game;
        this.activity = activity;

        gameStarted = true;

        htShapes = new Hashtable<>();

        // Get all the drawables and associate them with the shapes
        htShapes.put(Block.Shape.I, (NinePatchDrawable) ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.block_red));
        htShapes.put(Block.Shape.J, (NinePatchDrawable) ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.block_blue));
        htShapes.put(Block.Shape.L, (NinePatchDrawable) ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.block_orange));
        htShapes.put(Block.Shape.O, (NinePatchDrawable) ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.block_yellow));
        htShapes.put(Block.Shape.S, (NinePatchDrawable) ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.block_magenta));
        htShapes.put(Block.Shape.T, (NinePatchDrawable) ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.block_cyan));
        htShapes.put(Block.Shape.Z, (NinePatchDrawable) ContextCompat.getDrawable(activity.getBaseContext(), R.drawable.block_green));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        // Set up the thread when the surface is ready for it, if it hasn't been already
        if(thread == null)
        {
            thread = new GameThread(this);
            thread.setRunning(true);
            thread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        boolean retry = true;
        while(retry)
        {
            try
            {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {}
        }
        thread = null;
    }

    @Override
    protected void onDraw(Canvas canvas) { }

    public void update()
    {
        if(gameStarted)
            game.advanceFrame(activity.getInput());
    }

    public void render(Canvas canvas)
    {
        if(gameStarted)
        {
            if(game.getSoundEffectToPlay() > -1)
            {
                activity.playSound(game.getSoundEffectToPlay());
                game.clearSoundEffect();
            }
            if(getRedraw())
            {
                // Set up the rectangle width based on the canvas size
                rectWidth = canvas.getWidth() / 10;

                boolean sizeChanged = false;

                // If this is the first run, or the orientation changed, remake the bitmap
                if(stackState.getWidth() != canvas.getWidth())
                {
                    stackState = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
                    sizeChanged = true;
                }

                NinePatchDrawable tile;
                Block lastBlock = game.getLastBlock();

                if(game.getStackRedraw() || sizeChanged)
                {
                    // Draw the stack onto a bitmap so we can avoid drawing it over and over
                    Canvas stack = new Canvas(stackState);
                    stack.drawColor(Color.BLACK);

                    Block.Shape[][] colors = game.getStack();

                    // Paint the stack onto the canvas (and therefore onto the bitmap. Top two rows aren't drawn.
                    for(int i = 0; i <= 20; i++)
                        for(int j = 0; j < 10; j++)
                            // The canvas is already black so we don't have to draw that.
                            if(colors[i][j] != null)
                            {
                                tile = htShapes.get(colors[i][j]);
                                tile.setBounds(j * rectWidth,
                                        (20 - i) * rectWidth - rectWidth,
                                        j * rectWidth + rectWidth,
                                        (20 - i) * rectWidth);
                                tile.draw(stack);
                            }
                    game.clearLastBlock();
                }
                else if(lastBlock != null)
                {
                    // If a block was locked but no lines cleared, just draw that block onto the stack image
                    // No need to redraw the whole thing since the old tiles didn't change.
                    Canvas stack = new Canvas(stackState);
                    drawBlock(lastBlock, stack);
                    game.clearLastBlock();
                }

                canvas.drawBitmap(stackState, 0, 0, new Paint());

                // Paint the active piece onto the canvas
                Block currentBlock = game.getCurrentBlock();
                if(currentBlock != null)
                    drawBlock(currentBlock, canvas);

                // Update the views and play sounds
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        activity.setScore(game.getScore());
                        activity.setLevel(game.getLevel(), game.getMaxLevel());
                        activity.setNextPiece(game.getNextBlock().getShape());
                    }
                });

                // Tell the game we drew it
                game.clearRedraw();
            }

            if(game.getGameOver() != null)
            {
                // Stop the thread
                thread.setRunning(false);
                // Draw a message
                Paint paint = new Paint();
                paint.setTextSize(rectWidth);

                // shadow
                paint.setColor(Color.DKGRAY);
                canvas.drawText(context.getString(R.string.gameover), rectWidth + 2, rectWidth + 2, paint);
                canvas.drawText(context.getString(R.string.pressAny), rectWidth + 2, rectWidth * 3 + 2, paint);

                paint.setColor(Color.WHITE);
                canvas.drawText(context.getString(R.string.gameover), rectWidth, rectWidth, paint);
                canvas.drawText(context.getString(R.string.pressAny), rectWidth, rectWidth * 3, paint);
            }
        }
    }

    private void drawBlock(Block block, Canvas canvas)
    {
        NinePatchDrawable tile = htShapes.get(block.getShape());
        for (Point coord : block.getAbsoluteCoordinates())
        {
            tile.setBounds(coord.x * rectWidth,
                    (20 - coord.y) * rectWidth - rectWidth,
                    coord.x * rectWidth + rectWidth,
                    (20 - coord.y) * rectWidth);
            tile.draw(canvas);
        }
    }

    public GameManager getGame() { return game; }
    public void stop() {
        if(thread != null)
        {
            thread.setRunning(false);
        }
    }
    public void start()
    {
        if(thread != null)
        {
            thread.setRunning(true);
        }
        else
        {
            thread = new GameThread(this);
            thread.setRunning(true);
            thread.start();
        }
    }
    public boolean getRedraw() { return game.getPieceRedraw() || game.getStackRedraw(); }
}
