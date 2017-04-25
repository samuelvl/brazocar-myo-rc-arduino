/*
 * BrazoCar - Desarrollo de Aplicaciones Móviles
 * Curso 2015-2016
 * Desarrollado por Javier Martínez y Samuel Veloso
 */

package dam.brazocar;

import dam.brazocar.R;
import dam.brazocar.bluetoothScanActivities.*;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main Activity of the application
 * @author Javier Martínez Arrieta
 * @author Samuel Veloso López
 */
public class MainActivity extends Activity {
	
	private static final int REQUEST_ENABLE_BT = 0x05;
	private static final int MYO_DEVICES_ACTIVITY_CODE = 0x01;
	private static final int BT_DEVICES_ACTIVITY_CODE = 0x02;
	
	private BluetoothAdapter bluetoothAdapter;
	private String MAC_Myo_Device;
	private String MAC_RC_Device;

	/**
	 * Method called when MainActivity is created
	 * @param savedInstancetate - Parameters and other elements, values, etc that where saved when the activity was destroyed
	 */
	protected void onCreate(Bundle savedInstanceState) {

		/* Main layout is loaded */
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		/* Set custom title */
		final TextView title_view = (TextView) findViewById(R.id.title_id);
		title_view.setText(R.string.activity_main_name);
		title_view.setTypeface(Typeface.createFromAsset(getAssets(), "font/capsmall.ttf"));
		
		/* Check if Android device supports Bluetooth */
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(this.bluetoothAdapter == null) {
			Toast.makeText(getApplicationContext(), "Sorry, your device does not support Bluetooth", Toast.LENGTH_SHORT).show();			
		}
		else {
			/* Switch ON Bluetooth Adapter */
			SwitchOnBlueToothAdapter();
			
			/* When click "Pair Myo" button */
			Button button_Myo = (Button) findViewById(R.id.button_pair_myo);
			button_Myo.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                Intent intent = new Intent(getApplicationContext(), MyoScanActivity.class);	                
	                startActivityForResult(intent, MYO_DEVICES_ACTIVITY_CODE);
	            }
	        });
			
			/* When click "Pair RC-Car" button */
			Button button_RC = (Button) findViewById(R.id.button_pair_rc);
			button_RC.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                Intent intent = new Intent(getApplicationContext(), HelepolisScanActivity.class);
	                startActivityForResult(intent, BT_DEVICES_ACTIVITY_CODE);
	            }
	        });
			
			/* Start Manager Activity */
			Button button_GO = (Button) findViewById(R.id.button_open_manager);
			button_GO.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                Intent intent = new Intent(getApplicationContext(), ManagerActivity.class);
	                intent.putExtra(MyoScanActivity.MYO_MAC_ADDRESS, MAC_Myo_Device);
	                intent.putExtra(HelepolisScanActivity.BT_MAC_ADDRESS, MAC_RC_Device);
	                startActivity(intent);
	            }
	        });
			
			/* Close app */
			Button button_close = (Button) findViewById(R.id.button_close_app);
			// button_GO.setEnabled(true);
			button_close.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                finish();
	            }
	        });
		}
	}
	
	/**
	 *  Get data from sub-activities, which are HelepolisScanActivity and MyoScanActivity
	 *  @param requestCode - Code of request
	 *  @param resultCode - Result code
	 *  @param data - Intent containing necessary data to connect with a bluetooth device
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		switch(requestCode) {
		
			case MYO_DEVICES_ACTIVITY_CODE: {
				
				ImageView img = (ImageView) findViewById(R.id.img_pair_myo);
				Button button_GO = (Button) findViewById(R.id.button_open_manager);
				
				/* Update Myo MAC information */
				if(resultCode == Activity.RESULT_OK ) {
					/* Update MAC */
					MAC_Myo_Device = data.getExtras().getString(MyoScanActivity.MYO_MAC_ADDRESS);
					
					/* Update image state */
					img.setImageResource(R.drawable.main_ok_icon);
					
					/* Check if both MAC have been updated */
					if(MAC_RC_Device != null) {
						button_GO.setEnabled(true);
					}
				}
				/* Reset Myo MAC information */
				else {					
					MAC_Myo_Device = "";
					img.setImageResource(R.drawable.main_nok_icon);
					button_GO.setEnabled(false);					
				}
			} break;
			
			case BT_DEVICES_ACTIVITY_CODE: {
			
				ImageView img = (ImageView) findViewById(R.id.img_pair_rc);
				Button button_GO = (Button) findViewById(R.id.button_open_manager);
				
				/* Reset RC MAC information */
				if(resultCode == Activity.RESULT_OK ) {
					/* Update MAC */
					MAC_RC_Device = data.getExtras().getString(HelepolisScanActivity.BT_MAC_ADDRESS);
					
					/* Update image state */
					img.setImageResource(R.drawable.main_ok_icon);
					System.out.println(MAC_Myo_Device);
					/* Check if both MAC have been updated */
					if(MAC_Myo_Device != null) {
						button_GO.setEnabled(true);
					}
				}
				/* Reset BT MAC information */
				else {
					MAC_RC_Device = "";
					img.setImageResource(R.drawable.main_nok_icon);
					button_GO.setEnabled(false);					
				}
			} break;
			
			case REQUEST_ENABLE_BT: {
				SwitchOnBlueToothAdapter();
			} break;
		}
	}
	
	/**
	 * Returns if Bluetooth is currently enabled and ready for use or not.
	 * @return true if the local adapter is turned on, false otherwise
	 */
	private boolean SwitchOnBlueToothAdapter() {
		
		boolean result = this.bluetoothAdapter.isEnabled();
		
		if(!result) {
			Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
		}
		else {
			Toast.makeText(getApplicationContext(), "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
		}
		
		return result;
	}
}
