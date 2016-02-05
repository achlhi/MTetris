package ca.alexcomeau.texmobile.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread
{
    private final static int MAX_FPS = 30;
    private final static int MAX_FRAME_SKIPS = 5;
    private final static int FRAME_PERIOD = 1000 / MAX_FPS;
    private SurfaceHolder surfaceHolder;
    private GameView gameView;
    private boolean running;

    public GameThread(GameView gameView)
    {
        super();
        this.surfaceHolder = gameView.getHolder();
        this.gameView = gameView;
    }

    public void setRunning(boolean running) { this.running = running; }

    public void run()
    {
        Canvas canvas;

        long beginTime;
        long timeDiff;
        int sleepTime = 0;
        int framesSkipped;

        while(running)
        {
            canvas = null;

            try
            {
                beginTime = System.currentTimeMillis();
                framesSkipped = 0;

                // Do the game maths
                gameView.update();

                // Do the game arts if needed
                if(gameView.getRedraw()) canvas = surfaceHolder.lockCanvas();
                synchronized(surfaceHolder)
                {
                    if(canvas != null) gameView.render(canvas);

                    // How long did that take?
                    timeDiff = System.currentTimeMillis() - beginTime;

                    // How long do we need to sleep?
                    sleepTime = (int)(FRAME_PERIOD - timeDiff);

                    // We do need to sleep, right?
                    if(sleepTime > 0)
                    {
                        try
                        {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) { return; }
                    }

                    while(sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS)
                    {
                        // Do you like ketchup?
                        this.gameView.update();

                        // Did we catch up?
                        sleepTime += FRAME_PERIOD;
                        framesSkipped++;
                    }
                }
            }
            finally
            {
                if(canvas != null)
                    surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
}