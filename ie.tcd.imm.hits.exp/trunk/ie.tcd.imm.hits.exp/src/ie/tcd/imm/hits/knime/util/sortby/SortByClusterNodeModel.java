/*
 * All rights reserved. (C) Copyright 2009, Trinity College Dublin
 */
package ie.tcd.imm.hits.knime.util.sortby;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.base.node.mine.cluster.hierarchical.ClusterTreeModel;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * This is the model implementation of SortByCluster. Sorts the data by the
 * order defined by the clustering.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class SortByClusterNodeModel extends NodeModel {

	/** key for ascending order */
	static final String CFGKEY_ASCENDING = "ie.tcd.imm.hits.knime.util.sortby.ascending";
	/** default value for ascending order */
	static final boolean DEFAULT_ASCENDING = true;
	private final SettingsModelBoolean ascendingModel = new SettingsModelBoolean(
			CFGKEY_ASCENDING, DEFAULT_ASCENDING);

	/**
	 * Constructor for the node model.
	 */
	protected SortByClusterNodeModel() {
		super(new PortType[] { ClusterTreeModel.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final PortObject[] inData,
			final ExecutionContext exec) throws Exception {
		final BufferedDataTable data = (BufferedDataTable) inData[1];
		final ClusterTreeModel cluster = (ClusterTreeModel) inData[0];
		final DendrogramNode root = cluster.getRoot();
		final ArrayList<RowKey> keys = new ArrayList<RowKey>();
		getRowKeys(root, keys);
		final Map<RowKey, DataRow> map = new HashMap<RowKey, DataRow>(data
				.getRowCount() * 2);
		for (final DataRow dataRow : data) {
			map.put(dataRow.getKey(), dataRow);
		}
		final BufferedDataContainer container = exec.createDataContainer(data
				.getSpec());
		if (ascendingModel.getBooleanValue()) {
			for (int i = 0; i < keys.size(); ++i) {
				container.addRowToTable(map.get(keys.get(i)));
			}
		} else {
			for (int i = keys.size(); i-- > 0;) {
				container.addRowToTable(map.get(keys.get(i)));
			}
		}
		container.close();
		final BufferedDataTable ret = container.getTable();
		return new BufferedDataTable[] { ret };
	}

	@Deprecated
	private RowKey getKey(final DendrogramNode node) {
		if (node.getLeafDataPoint() != null) {
			return node.getLeafDataPoint().getKey();
		}
		RowKey key;
		try {
			final Method getKey = node.getClass().getMethod("getLeafRowKey");
			key = (RowKey) getKey.invoke(node);
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		return key;
	}

	private void getRowKeys(final DendrogramNode node, final List<RowKey> ids) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			final RowKey key = getKey(node);
			ids.add(key);
			return;
		}
		getRowKeys(node.getFirstSubnode(), ids);
		getRowKeys(node.getSecondSubnode(), ids);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		if (!(inSpecs[0] instanceof DataTableSpec)) {
			throw new InvalidSettingsException(
					"Wrong type of input in first port.");
		}
		return new DataTableSpec[] { (DataTableSpec) inSpecs[1] };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		ascendingModel.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		ascendingModel.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		ascendingModel.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Do nothing
	}

}
