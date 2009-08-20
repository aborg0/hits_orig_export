/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package ie.tcd.imm.hits.exp;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * This is the eclipse bundle activator. Note: KNIME node developers probably
 * won't have to do anything in here, as this class is only needed by the
 * eclipse platform/plugin mechanism. If you want to move/rename this file, make
 * sure to change the plugin.xml file in the project root directory accordingly.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class LabsNodePlugin extends Plugin {

	/** Make sure that this *always* matches the ID in plugin.xml. */
	public static final String PLUGIN_ID = "ie.tcd.imm.hits.exp";

	// The shared instance.
	private static LabsNodePlugin plugin;

	/**
	 * The constructor.
	 */
	@edu.umd.cs.findbugs.annotations.SuppressWarnings("ST")
	public LabsNodePlugin() {
		super();
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation.
	 * 
	 * @param context
	 *            The OSGI bundle context
	 * @throws Exception
	 *             If this plugin could not be started
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

	}

	/**
	 * This method is called when the plug-in is stopped.
	 * 
	 * @param context
	 *            The OSGI bundle context
	 * @throws Exception
	 *             If this plugin could not be stopped
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return Singleton instance of the Plugin
	 */
	public static LabsNodePlugin getDefault() {
		return plugin;
	}
}
