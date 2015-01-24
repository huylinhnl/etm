package com.holster.etm.gui.jee.configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.holster.etm.core.EtmException;
import com.holster.etm.core.configuration.EtmConfiguration;
import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ConfigurationProducer.class);

	private EtmConfiguration etmConfiguration;

	@Produces
	@GuiConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.etmConfiguration == null) {
				InputStream settingsStream = null;
				try {
					File file = new File("etm.properties");
					if (file.exists() && file.canRead()) {
						settingsStream = new FileInputStream(file);
					} else {
						settingsStream = getClass().getResourceAsStream("/etm.properties");
					}
					this.etmConfiguration = new EtmConfiguration();
					if (settingsStream != null) {
						this.etmConfiguration.load(settingsStream);
					}
				} catch (IOException e) {
					throw new EtmException(EtmException.CONFIGURATION_LOAD_EXCEPTION, e);
				} finally {
					if (settingsStream != null) {
						try {
	                        settingsStream.close();
                        } catch (IOException e) {
	                        if (log.isWarningLevelEnabled()) {
	                        	log.logDebugMessage("Unable to close configuration file.", e);
	                        }
                        }
					}
				}
			}
		}
		return this.etmConfiguration;
	}
}
