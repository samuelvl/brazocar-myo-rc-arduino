/*
 * BrazoCar - Desarrollo de Aplicaciones Móviles
 * Curso 2015-2016
 * Desarrollado por Javier Martínez y Samuel Veloso
 */

package dam.brazocar;

import dam.brazocar.bluetoothScanActivities.*;

import com.thalmic.myo.*;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * Manager Activity of the application that process data from myo to send movements to the RC car
 * @author Javier Martínez Arrieta
 * @author Samuel Veloso López
 */
public class ManagerActivity extends Activity {
	
    public static final int BACKWARDS = 0x00;
    public static final int FORWARDS = 0x01;
    public static final int STOPPED = 0x02; 

	private String MAC_Myo_Device;
	private String MAC_RC_Device;
	
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothDevice rc_device;
	private MyoListener myo_listener;
	private HelepolisListener rc_listener;
	
	private ImageView myo_connection_state_img;
	private ImageView rc_connection_state_img;
	private ImageView myo_gesture_img;
	
	private TextView myo_left_speed_text;
	private TextView myo_speed_text;
	private TextView myo_right_speed_text;
	
	/**
	 * Method called when MainActivity is created
	 * @param savedInstancetate - Parameters and other elements, values, etc that where saved when the activity was destroyed
	 */
	protected void onCreate(Bundle savedInstanceState) {
		
		/* Manager layout is loaded */
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.manager_activity);
		
		/* Set custom title */
		final TextView title_view = (TextView) findViewById(R.id.title_id);
		title_view.setText(R.string.activity_manager_name);
		title_view.setTypeface(Typeface.createFromAsset(getAssets(), "font/capsmall.ttf"));
		title_view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 55);
		
		/* Get layout resources */
		myo_connection_state_img = (ImageView) findViewById(R.id.myo_connection_state_img);
		rc_connection_state_img = (ImageView) findViewById(R.id.rc_connection_state_img);
		myo_gesture_img = (ImageView) findViewById(R.id.myo_gesture);
		
		myo_left_speed_text = (TextView) findViewById(R.id.text_left_speed);
		myo_speed_text = (TextView) findViewById(R.id.text_speed);
		myo_right_speed_text = (TextView) findViewById(R.id.text_right_speed);
		myo_speed_text.setTypeface(Typeface.createFromAsset(getAssets(), "font/lcd.ttf"));
		
		/*Avoids screen lock*/
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		/* Init Bluetooth */
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		/* Init Myo Armband HUB */
		Hub hub = Hub.getInstance();
		if (!hub.init(this)) {
			Toast.makeText(getApplicationContext(), "Could not initialize the HUB", Toast.LENGTH_SHORT).show();
		    finish();
		}
		
		Hub.getInstance().setLockingPolicy(Hub.LockingPolicy.NONE);
		Hub.getInstance().setSendUsageData(false);
		
		/* Connect to Myo as a server */
		MAC_Myo_Device = getIntent().getExtras().getString(MyoScanActivity.MYO_MAC_ADDRESS);
		hub.attachByMacAddress(MAC_Myo_Device);		
		connectMyo();		
		
		/* Connect to RC-Car as a master */
		MAC_RC_Device = getIntent().getExtras().getString(HelepolisScanActivity.BT_MAC_ADDRESS);		
		rc_device = bluetoothAdapter.getRemoteDevice(MAC_RC_Device);
		connectHelepolis();
	}
	
	/**
	 * Method to connect with the myo device
	 */
	private void connectMyo() {
		if(myo_listener != null) {
			Hub.getInstance().removeListener(myo_listener);
		}
		myo_listener = new MyoListener();
		Hub.getInstance().addListener(myo_listener);
	}
	
	/**
	 * Method to connect with the RC device
	 */
	private final void connectHelepolis() {
		if((rc_listener != null) && (rc_listener.isAlive())) {
			rc_listener.onDestroy();
		}
		rc_listener = new HelepolisListener();
		rc_listener.start();
	}
	
	/**
	 * Listener that manages Myo state changes 
	 */
	private class MyoListener extends AbstractDeviceListener {
		
		private boolean isConnected;
		
		/**
		 * Main constructor
		 */
		public MyoListener() {
			super();
			isConnected = false;			
		}
	    
		/**
		 * Method called when a myo armband device is connected with the mobile phone
		 */
	    public void onConnect(Myo myo, long timestamp) {
	    	myo_connection_state_img.setImageResource(R.drawable.manager_myo_ok);
	    	isConnected = true;
	    }
	    
	    /**
		 * Method called when a myo armband device is disconnected from the mobile phone
		 */
	    public void onDisconnect(Myo myo, long timestamp) {
	    	myo_connection_state_img.setImageResource(R.drawable.manager_myo_nok);
	    	isConnected = false;
	    }
	    
	    /** 
	     * Handles the different poses
	    */
	    public void onPose(Myo myo, long timestamp, Pose pose) {
            switch (pose) {
                case DOUBLE_TAP:
                	myo_gesture_img.setImageResource(R.drawable.manager_myo_gesture_tap);
                	rc_listener.latchHelepolis();
                    break;
                case FIST:
                	myo_gesture_img.setImageResource(R.drawable.manager_myo_gesture_fist);
                    break;
                case WAVE_IN:
                	myo_gesture_img.setImageResource(R.drawable.manager_myo_gesture_left);
                    break;
                case WAVE_OUT:
                	myo_gesture_img.setImageResource(R.drawable.manager_myo_gesture_right);
                    break;
                case FINGERS_SPREAD:
                	myo_gesture_img.setImageResource(R.drawable.manager_myo_gesture_spread);
                    break;
                case REST:
                	myo_gesture_img.setImageResource(R.drawable.manager_myo_gesture_ready);
                    break;
                default:
                	myo_gesture_img.setImageResource(R.drawable.manager_myo_gesture_ready);
            }

            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                /* Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                   hold the poses without the Myo becoming locked. */
                myo.unlock(Myo.UnlockType.HOLD);

                /* Notify the Myo that the pose has resulted in an action, in this case changing
                   the text on the screen. The Myo will vibrate. */
                myo.notifyUserAction();
            }
            else {
                /* Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                   stay unlocked while poses are being performed, but lock after inactivity. */
                myo.unlock(Myo.UnlockType.TIMED);
            }
	    }
	    
	    /**
	     * Method called when orientation data has changed
	     * @param myo - Myo armband device
	     * @param timestamp - Information about the time, in milliseconds, when data has been taken
	     * @param rotation - Information about the rotation angle
	     */
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
        	
            /* Calculate Euler angles (roll, pitch, and yaw) from the quaternion. */
            int roll = (int) Math.toDegrees(Quaternion.roll(rotation));
            int pitch = (int) Math.toDegrees(Quaternion.pitch(rotation));

            /* Adjust roll for the orientation of the Myo on the arm */
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW)  roll *= -1;
            
            /* Compute movement */
            rc_listener.computeMovement(roll, pitch);
        }
        
        /**
         * Informs if myo armband device is connected or not
         * @return isConnected - True if myo is connected
         */
        public boolean isConnected() {
        	return isConnected;
        }
	};
	
	/**
	 *  Thread that sends the direction and the speed of each wheel according to the collected information 
	 */
	private class HelepolisListener extends Thread {
		
		/* Movement constants */
		private static final float MIN_SPEED = 100;
		private static final float MAX_SPEED = 255;
		private static final float MAX_PITCH = 89;
		private static final float MAX_ROLL = 100;
		private static final float MIN_ROLL = 0;
		
		/* Connection vars */
		private BluetoothSocket socket;
		private OutputStream output_buffer;
		private InputStream input_buffer;
		
		/* Physics engine vars */
		private boolean isLocked;
		private int direction;
		private int speed;
		private int rightWheelSpeed;
		private int leftWheelSpeed;
		
		/* Thread control */
		private boolean threadIsRunning;
		
		/**
		 * Main constructor
		 */
		public HelepolisListener() {
			
			isLocked = true;
			direction = STOPPED;
			speed = rightWheelSpeed = leftWheelSpeed = 0;		
			threadIsRunning = true;
			
			try {
	            Method m = rc_device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
	            socket = (BluetoothSocket) m.invoke(rc_device, 1);
	            
	            /* Cancel discovery because it will slow down the connection */
				bluetoothAdapter.cancelDiscovery();
	            
	            socket.connect();
	            while(!socket.isConnected());
	    		rc_connection_state_img.setImageResource(R.drawable.manager_arduino_ok);
	    		
	            //Gets the output and input streams created in the socket
	    		output_buffer = socket.getOutputStream();
	    		input_buffer = socket.getInputStream();
			}
			catch(Exception e) { System.err.println("Error when connecting to RC car."); }
		}
		
		/**
		 * Main method of the thread
		 */
		public void run() {
			while(threadIsRunning) {
				if(myo_listener.isConnected() && socket.isConnected()) {
					if((input_buffer != null) && (output_buffer != null)) {
						try {
							if(!this.isLocked) {								
								output_buffer.write((byte) direction);
								output_buffer.write((byte) leftWheelSpeed);
								output_buffer.write((byte) rightWheelSpeed);
							}
							else {
								output_buffer.write((byte) STOPPED);
								output_buffer.write((byte) 0);
								output_buffer.write((byte) 0);
							}
							while(input_buffer.available() <= 0);
				            synchronized(this) { this.wait(100); }
			            }
			            catch(Exception e) { System.err.println("Error when IO to Helepolis."); }
					}
				}
				else {
					if(!socket.isConnected()) connectHelepolis();
				}
			}
		}
		
		/**
		 * Unlocks the RC device to start moving
		 */
		public void latchHelepolis() {
			isLocked = !isLocked;
		}
		
		/**
		 * Calculates the speed for each wheel according to collected data
		 * @param roll - Roll angle (left and right movements)
		 * @param pitch - Pitch angle (up and down movements)
		 */
		public void computeMovement(float roll, float pitch) {
			
			/* Set direction */
			if(pitch >= 0) { direction = FORWARDS; }
			else { direction = BACKWARDS; }
			
			/* Compute general speed */
			speed = (int) ((MAX_SPEED / Math.sqrt(MAX_PITCH)) * Math.sqrt(Math.abs(pitch)));
			if(speed > MAX_SPEED) speed = (int) MAX_SPEED;
			else if(speed < MIN_SPEED) speed = (int) MIN_SPEED;
			
			/* Compute each wheel speed according to roll angle */
			if(roll > MAX_ROLL) roll = MAX_ROLL;
			else if(roll < MIN_ROLL) roll = MIN_ROLL;
			
			int right_wheel_weight = 100;
			int left_wheel_weight = 100;
			
			/* Right side */
			if(roll > (MAX_ROLL / 2)) {
				right_wheel_weight = (int) (2 * ((MAX_ROLL - roll) / MAX_ROLL) * 100);
			}
			/* Left side */
			else {
				left_wheel_weight = (int) (2 * (roll / MAX_ROLL) * 100);
			}
			
			rightWheelSpeed = (int) (speed * ((float) right_wheel_weight/ 100f));
			leftWheelSpeed = (int) (speed * ((float) left_wheel_weight / 100f));
			
			/* Update GUI */
			if(!isLocked)
			{
				myo_left_speed_text.setText(left_wheel_weight + "%");
				myo_speed_text.setText(speed + " m/s");
				myo_right_speed_text.setText(right_wheel_weight + "%");
			}
			else
			{
				myo_left_speed_text.setText("-	-	-");
				myo_speed_text.setText("LOCKED");
				myo_right_speed_text.setText("-	-	-");
			}
			
			// System.out.println("Direction[" + direction + "]" + "S[" + speed + "] : L[" + leftWheelSpeed + "] : R[" + rightWheelSpeed + "]");
		}
		
	    /**
	     * Method called when the activity is finishing
	     */
		public void onDestroy() {
			
			/* Stop thread */
			threadIsRunning = false;
			this.interrupt();
			
			try {
				/* Suspend Helepolis movement */
				output_buffer.write((byte) STOPPED);
				output_buffer.write((byte) 0);
				output_buffer.write((byte) 0);
				while(input_buffer.available() <= 0);
				
				/* Close buffers */
				socket.close();
				input_buffer.close();
				output_buffer.close();
				
				/* Empty buffers */
				input_buffer = null;
				output_buffer = null;
			}
			catch (IOException e) { System.err.println("Error when closing Helepolis connection."); }
		}
		
	};
	
	protected void onDestroy() {
        
        /* Close Myo connection */
        if(myo_listener != null) {
			Hub.getInstance().removeListener(myo_listener);
		}
        
        /* Close Helepolis connection */
        if(rc_listener != null) {
        	rc_listener.onDestroy();
        }

        /* Close Hub connection */
        if (isFinishing()) { Hub.getInstance().shutdown(); }
        
        super.onDestroy();
        
    }

}
