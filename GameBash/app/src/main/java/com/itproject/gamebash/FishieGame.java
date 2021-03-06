package com.itproject.gamebash;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class FishieGame {
    final int TOTAL_LIVES = 3;
    final int BLINK_DURATION = 6; // Persists over 6 screen draws (not seconds)

    boolean isGameOver = false;

    int canvasWidth, canvasHeight;
    boolean touch = false;
    int livesLeft = TOTAL_LIVES;
    int blinkCount = 0;
    int score = 0;

    // Fish
    Fish fish;
    int y_min = -1, y_max = -1;

    // Balls
    Ball redBall, whiteBall, yellowBall;
    Ball[] balls;

    ContentView contentView;
    Sounds sounds;
    Vibrator vibrator;

    FishieGame(AppCompatActivity activity) {
        fish = new Fish(
                BitmapFactory.decodeResource(activity.getResources(), R.drawable.fish_at_rest),
                BitmapFactory.decodeResource(activity.getResources(), R.drawable.fish_on_tap)
        );

        balls = new Ball[]{
                redBall = new Ball(Color.RED, 40, 0, 23, true),
                whiteBall = new Ball(Color.WHITE, 30, 2, 20, false),
                yellowBall = new Ball(Color.YELLOW, 20, 1, 16, false)
        };

        contentView = new ContentView(activity);
        sounds = new Sounds(activity);
        vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
    }

    class Fish {
        final int SPEED_INCREASE = 3;
        final int ON_TOUCH_SPEED = -36;

        final Bitmap AT_REST;
        final Bitmap ON_TAP;

        final int WIDTH;
        final int HEIGHT;

        int x = 15;
        int y = 550;
        int speed = 0;


        Fish(Bitmap atRest, Bitmap onTap) {
            this.AT_REST = atRest;
            this.ON_TAP = onTap;

            WIDTH = AT_REST.getWidth();
            HEIGHT = AT_REST.getHeight();
        }


        boolean didEat(Ball ball) {
            return (x < ball.x && ball.x < (x + WIDTH) &&
                    y < ball.y && ball.y < (y + HEIGHT));
        }


        void updatePosition() {
            increaseSpeed();
            y_min = fish.HEIGHT;
            y_max = canvasHeight - fish.HEIGHT * 3;

            y = y + speed;
            if (y > y_max) {
                y = y_max;
            } else if (y < y_min) {
                y = y_min;
            }
        }


        void increaseSpeed() {
            speed += SPEED_INCREASE;
        }


        void setSpeedTo_ON_TOUCH_SPEED() {
            speed = ON_TOUCH_SPEED;
        }

    }


    class Ball {
        private final int WIDTH_OFFSET = 21;
        private final int X_WHEN_EATEN = -100;

        final int COLOR;
        final int RADIUS;
        final int POINTS;
        final int SPEED;
        final Boolean IS_DANGEROUS;

        final Paint PAINT;

        int x, y;


        Ball(int color, int radius, int points, int speed, Boolean isDangerous) {
            this.COLOR = color;
            this.RADIUS = radius;
            this.POINTS = points;
            this.SPEED = speed;
            this.IS_DANGEROUS = isDangerous;

            PAINT = new Paint();
            PAINT.setColor(color);
            PAINT.setAntiAlias(false);

            x = y = 0;
        }


        void eaten() {
            Sounds.play(sounds.eat);
            if(IS_DANGEROUS) {
                vibrator.vibrate(200);
                --livesLeft;
                if (livesLeft == 0) {
                    isGameOver = true;
                } else {
                    ++blinkCount;
                }
            } else {
                score += POINTS;
            }

            x = X_WHEN_EATEN;
        }


        void updatePosition() {
            x -= SPEED;
            if (x < 0) {
                x = canvasWidth + WIDTH_OFFSET;
                y = (int) (Math.floor(Math.random() * (y_max - y_min)) + y_min);
            }
        }

    }

    class ContentView extends View {
        final float scale;

        Bitmap background;
        Bitmap redHeart;

        Paint scorePaint;

        int[] heartPositions = new int[TOTAL_LIVES];

        public ContentView(Context context) {
            super(context);

            // Get the screen's density scale
            scale = getResources().getDisplayMetrics().density;

            background = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
            redHeart = BitmapFactory.decodeResource(getResources(), R.drawable.hearts);

            scorePaint = new Paint();
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTextSize(70);
            scorePaint.setTypeface(Typeface.DEFAULT_BOLD);
            scorePaint.setAntiAlias(true);
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvasWidth = getWidth();
            canvasHeight = getHeight();
            for(int i = 1; i <= TOTAL_LIVES; ++i) {
                // canvasWidth - dp_to_px(5) -> will give a space of 5dp from the edge of canvas
                // redHeart.getWidth() + dp_to_px(5) -> will give a space of 5dp after each heart
                // Therefore there is (a total of 10dp between last heart and canvas) && (5dp space between each heart)
                heartPositions[i-1] = (canvasWidth - dp_to_px(5)) - (i * (redHeart.getWidth() + dp_to_px(5)));
            }

            canvas.drawBitmap(background, 0, 0, null);

            drawFishOn(canvas);
            drawBallsOn(canvas);

            // Draw red hearts
            if (blinkCount > BLINK_DURATION) {
                blinkCount = 0;
            }
            // (blinkCount -1)/2 -> [{1,2}->0], [{3,4}->1], [{5}->2]
            for (int i = 0; i < livesLeft + ((blinkCount - 1)/2)%2; ++i) {
                canvas.drawBitmap(redHeart, heartPositions[i], 40, null);
                if(blinkCount > 0) {
                    ++ blinkCount;
                }
            }

            // x = 10dp, y = 40dp
            canvas.drawText("Score: " + score, dp_to_px(10), dp_to_px(40), scorePaint);
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touch = true;
                fish.setSpeedTo_ON_TOUCH_SPEED();
            }
            return true;
        }

        void drawFishOn(Canvas canvas) {
            fish.updatePosition();
            if (touch) {
                canvas.drawBitmap(fish.ON_TAP, fish.x, fish.y, null);
                touch = false;
            } else {
                canvas.drawBitmap(fish.AT_REST, fish.x, fish.y, null);
            }
        }

        void drawBallsOn(Canvas canvas) {
            for (FishieGame.Ball ball : balls) {
                ball.updatePosition();
                if (fish.didEat(ball)) {
                    ball.eaten();
                }
                canvas.drawCircle(ball.x, ball.y, ball.RADIUS, ball.PAINT);
            }
        }


        int dp_to_px(int dp) {
            // Convert the dps to pixels, based on density scale
            return (int) (dp * scale + 0.5f);
        }

    }
}
