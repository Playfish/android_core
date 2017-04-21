
package org.ros.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.GridLayout;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.android.android_15.R;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.Timer;
import java.util.TimerTask;

import geometry_msgs.Twist;
import nav_msgs.Odometry;

/**
 * Created by root on 17-3-26.
 */
public class VirtualKeyboardView extends GridLayout implements AnimationListener,
        MessageListener<nav_msgs.Odometry>, NodeMain {

    /**
     * Up, Down, Left, Right Button for control mobile base and
     * send sets up the visual elements of the virtual keyboard.
     */
    private Button upButton;
    private Button downButton;
    private Button leftButton;
    private Button rightButton;
    private Button stopButton;

    /**
     * Used to publish velocity commands at a specific rate.
     */
    private Timer publisherTimer;
    private geometry_msgs.Twist currentVelocityCommand;
    private String topicName;

    private Publisher<Twist> publisher;

    /**
     * {@code true} if the keyboard should publish linear velocities along the Y
     * axis instead of angular velocities along the Z axis, {@code false}
     * otherwise.
     */
    private boolean holonomic;

    /**
     * Velocity commands are published when this is true. Not published otherwise.
     * This is to prevent spamming velocity commands.
     */
    private volatile boolean publishVelocity;

    /**
     * mainLayout The parent layout that contains all the elements of the virtual
     * keyboard.
     */
    private GridLayout mainLayout;

    /**
     * parentSize The length (width==height ideally) of a side of the parent
     * container that holds the virtual keyboard.
     */
    private float parentSize = Float.NaN;

    private static final int INVALID_POINTER_ID = -1;

    /**
     * pointerId Used to keep track of the contact that initiated the interaction
     * with the virtual keyboard. All other contacts are ignored.
     */
    private int pointerId = INVALID_POINTER_ID;

    public VirtualKeyboardView(Context context) {
        super(context);
        initVirtualKeyboard(context);
        topicName = "/mybot/cmd_vel";
    }

    /**
     * Sets up the visual elements of the virtual Keyboard.
     */
    public VirtualKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVirtualKeyboard(context);
        topicName = "/mybot/cmd_vel";
    }

    public VirtualKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        topicName = "/mybot/cmd_vel";
    }

    /**
     * @param enabled
     *          {@code true} if this keyboard should publish linear velocities
     *          along the Y axis instead of angular velocities along the Z axis,
     *          {@code false} otherwise
     */
    public void setHolonomic(boolean enabled) {
        holonomic = enabled;
    }

    /**
     * Initialize the fields with values that can only be determined once the
     * layout for the views has been determined.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Call the parent's onLayout to setup the views.
        super.onLayout(changed, l, t, r, b);
        // The parent container must be a square. A square container simplifies the
        // code. A non-square container does not provide any benefit over a
        // square.
        if (mainLayout.getWidth() != mainLayout.getHeight()) {
            // TODO(munjaldesai): Need to throw an exception/error. For now the
            // touch events will not be processed.
            this.setOnTouchListener(null);
        }
        parentSize = mainLayout.getWidth();
        if (parentSize < 200 || parentSize > 400) {
            // TODO: Need to throw an exception for attempting to create
            // a virtual keyboard that is either too small or too big. For now the
            // touch events will be processed.
            this.setOnTouchListener(null);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

    }


    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public void onNewMessage(final nav_msgs.Odometry message) {
        double heading;
        // For some reason the values of z and y seem to be interchanged. If they
        // are not swapped then heading is always incorrect.
        double w = message.getPose().getPose().getOrientation().getW();
        double x = message.getPose().getPose().getOrientation().getX();
        double y = message.getPose().getPose().getOrientation().getZ();
        double z = message.getPose().getPose().getOrientation().getY();
        heading = Math.atan2(2 * y * w - 2 * x * z, x * x - y * y - z * z + w * w) * 180 / Math.PI;

    }


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_15/virtual_keyboard_view");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        System.out.print("onStart");
        publisher = connectedNode.newPublisher(topicName, geometry_msgs.Twist._TYPE);
        currentVelocityCommand = publisher.newMessage();
        Subscriber<Odometry> subscriber =
                connectedNode.newSubscriber("odom", nav_msgs.Odometry._TYPE);
        subscriber.addMessageListener(this);
        publisherTimer = new Timer();
        publisherTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (publishVelocity) {
                    publisher.publish(currentVelocityCommand);
                }
            }
        }, 0, 80);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {

                upButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;
                        publishVelocity(0.2,-0,0);
                    }
                });
                downButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setBackgroundColor(0);
                        publishVelocity = true;
                        publishVelocity(-0.2,-0,0);
                    }
                });
                leftButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;
                        publishVelocity(0,-0,-1);

                    }
                });
                rightButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;

                        publishVelocity(0,-0,1);
                    }
                });

                stopButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;

                        publishVelocity(0,0,0);
                    }
                });
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                // Get the coordinates of the pointer that is initiating the
                // interaction.
                pointerId = event.getPointerId(event.getActionIndex());
                // If the current contact is close to the location of the contact prior
                // to contactUp.
                upButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;
                        publishVelocity(0.2,-0,0);
                    }
                });
                downButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;
                        publishVelocity(-0.2,-0,0);
                    }
                });
                leftButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;
                        publishVelocity(0,-0,-1);

                    }
                });
                rightButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;

                        publishVelocity(0,-0,1);
                    }
                });
                stopButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publishVelocity = true;

                        publishVelocity(0,0,0);
                    }
                });

                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                publishVelocity(0, 0, 0);
                // Stop publishing the velocity since the contact is no longer on the
                // screen.
                publishVelocity = false;
                publisher.publish(currentVelocityCommand);
                break;
            }
        }
        return true;
    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {
        publisherTimer.cancel();
        publisherTimer.purge();

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }
    /**
     * Sets up the visual elements of the virtual keyboard.
     */
    private void initVirtualKeyboard(Context context) {
        System.out.print("initVirtualKeyboard");
        // Instantiate the elements from the layout XML file.
        LayoutInflater.from(context).inflate(R.layout.virtual_keyboard, this, true);
        mainLayout = (GridLayout) findViewById(R.id.virtual_keyboard_layout);
        upButton = (Button) findViewById(R.id.button_up);
        downButton = (Button) findViewById(R.id.button_down);
        leftButton = (Button) findViewById(R.id.button_left);
        rightButton = (Button) findViewById(R.id.button_right);
        stopButton = (Button) findViewById(R.id.button_stop);

        holonomic = false;
    }

    /**
     * Publish the velocity as a ROS Twist message.
     *
     * @param linearVelocityX
     *          The normalized linear velocity (-1 to 1).
     * @param angularVelocityZ
     *          The normalized angular velocity (-1 to 1).
     */
    private void publishVelocity(double linearVelocityX, double linearVelocityY,
                                 double angularVelocityZ) {
        currentVelocityCommand.getLinear().setX(linearVelocityX);
        currentVelocityCommand.getLinear().setY(-linearVelocityY);
        currentVelocityCommand.getLinear().setZ(0);
        currentVelocityCommand.getAngular().setX(0);
        currentVelocityCommand.getAngular().setY(0);
        currentVelocityCommand.getAngular().setZ(-angularVelocityZ);
    }
}
