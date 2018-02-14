/**
 *  Temp-Based Ecobee Mode
 *
 *  Copyright 2018 Justin Leonard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0  
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 */
def getVersionNum() { return "1.0.0" }
private def getVersionLabel() { return "Temp-Based Ecobee Mode, version ${getVersionNum()}" }

definition(
	name: "Temp-Based Ecobee Mode",
	namespace: "lnjustin",
	author: "Justin J. Leonard",
	description: "Set Ecobee Heat/Cool/Auto Mode Based on Outside Temp",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Setup ${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "Name for Mode Handler") {
        	label title: "Name thisHandler", required: true, defaultValue: "Smart Mode"
        }
        
        section(title: "Select Thermostat") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled as requested. Turn back on to activate handler."}
        	else {input ("thermostat", "capability.Thermostat", title: "Pick Ecobee Thermostat", required: true, multiple: false, submitOnChange: true)}            
		}

        if (!settings.tempDisable) {
               section(title: "When the temperature is at or above...") {
        			input(name: "lowerThreshold", title: "Lower Threshold (Optional)", type: 'number', required: false)
        		}

        		section(title: "When the temperature is at or below...") {
            		input(name: "upperThreshold", title: "Upper Threshold (Optional)", type: 'number', required: false)
				}
        
				section(title: "Set Thermostat Mode To...") {
        			input(name: "mode", type: "enum", title: "Pick Thermostat Mode", 
                		metadata:[values:['heat','cool','off','auto']],
                   		required: true, multiple: false, submitOnChange: true)
        		}      
                section(title: "Notifications") {
					input(name:"sendPushMessage", type: "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], 
    				required: false)
				}

        }
        section(title: "Temporary Disable") {
        	input(name: "tempDisable", title: "Temporarily Disable this Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        section (getVersionLabel()) {}
    }
}

def installed() {
	initialize()  
}

def updated() {
	unsubscribe()
    initialize()
}

def initialize() {

	if(tempDisable == true) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
	
	subscribe( settings.thermostat, "weatherTemperature", weatherChangeHandler)
}

def weatherChangeHandler(evt) {
	int newTemperature = evt.value as int
    String switchToMode = settings.mode
    def message = "Ecobee Switched to ${switchToMode}"
    
    if (settings.lowerThreshold != null && settings.upperThreshold != null) {
    	if (newTemperature >= settings.lowerThreshold && newTemperature <= settings.upperThreshold) { 
      	  	thermostat.setThermostatMode(switchToMode) 
        	send(message)
        }
    }
    else if (settings.lowerThreshold != null) {
    	if (newTemperature >= settings.lowerThreshold) { 
        	thermostat.setThermostatMode(switchToMode)
            send(message)
         }
    }
    else if (settings.upperThreshold != null) {
    	if (newTemperature <= settings.upperThreshold) { 
       		 thermostat.setThermostatMode(switchToMode) 
             send(message)
        }
    }

}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}

private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)
	}
	log.debug msg
}
