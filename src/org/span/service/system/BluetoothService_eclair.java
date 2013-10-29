/**
 *  SPAN - Smart Phone Ad-Hoc Networking project
 *  Copyright (c) 2012 The MITRE Corporation.
 */
/**
 *  Portions of this code are copyright (c) 2009 Harald Mueller and Sofia Lemons.
 * 
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 */
package org.span.service.system;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

public class BluetoothService_eclair extends BluetoothService {

	BluetoothAdapter btAdapter = null;
	
	public BluetoothService_eclair() {
		super();
        btAdapter = BluetoothAdapter.getDefaultAdapter();     
	}
	
	@Override
	public boolean startBluetooth() {
		boolean connected = false;
		this.btAdapter.enable();
		/**
		 * TODO: Not sure if that loop is needed anymore. 
		 * Looks like that bt is coming-up more reliable with Android 2.0
		 */
		int checkcounter = 0;
		while (connected == false && checkcounter <= 60) {
			// Wait up to 60s for bluetooth to come up.
			// does not behave unless started after BT is enabled.
			connected = this.btAdapter.isEnabled();
			if (connected == false) {
				checkcounter++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Nothing
				}
			} else {
				break;
			}
		}
		return connected;
	}

	@Override
	public boolean stopBluetooth() {
		return this.btAdapter.disable();
	}

	@Override
	public boolean isBluetoothEnabled() {
		return this.btAdapter.isEnabled();
	}

	@Override
	public void setApplication(Application application) {
		// unneeded - not implemented
	}
}
