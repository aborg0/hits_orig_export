/**
 * 
 */
package ie.tcd.imm.hits.knime.view.dendrogram.viewonly;

import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.util.Misc;
import ie.tcd.imm.hits.util.swing.colour.ColourComputer;
import ie.tcd.imm.hits.util.swing.colour.ColourSelector.ColourModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTreeNode;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramDrawingPane;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint;
import org.knime.base.node.viz.plotter.dendrogram.BinaryTree.Traversal;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

/**
 * Drawing pane for the heatmap with dendrogram node.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
@DefaultAnnotation( { Nonnull.class, CheckReturnValue.class })
public class HeatmapDendrogramDrawingPane extends DendrogramDrawingPane {
	private static final long serialVersionUID = 5198699225298295730L;
	private static final float BOLD = 2.0f;
	private BinaryTree<DendrogramPoint> rootNode;
	private float lineThickness = 1.0f;
	private DendrogramNodeModel nodeModel;
	private int[] indices;
	private int[] selectedIndices;
	private ColourModel colourModel;
	private int cellHeight;
	private int cellWidth = 20;
	private final List<String> selectedColumns = new ArrayList<String>();
	private final List<String> visibleColumns = new ArrayList<String>();
	private int maxStringLength;
	private String[] keys;
	private boolean directionLeftToRight;

	private int leafX;
	private boolean showValues;

	// private boolean directionUpToDown;

	/** Constructs the drawing pane. */
	public HeatmapDendrogramDrawingPane() {
		super();
	}

	/**
	 * @param nodeModel
	 *            the new {@link DendrogramNodeModel}.
	 */
	public void setNodeModel(final DendrogramNodeModel nodeModel) {
		this.nodeModel = nodeModel;
		maxStringLength = 0;
		if (this.nodeModel != null) {
			computeIndices();
			for (final DataRow row : this.nodeModel.getOrigData()) {
				maxStringLength = Math.max(maxStringLength, getFontMetrics(
						getFont()).stringWidth(row.getKey().getString()));
			}
		}
		selectedColumns.clear();
		selectedColumns.addAll(this.nodeModel.getSelectedColumns());
	}

	/**
	 * @return The maximal length of {@link RowKey}s in pixels.
	 */
	public int getMaxStringLength() {
		return maxStringLength;
	}

	private void computeIndices() {
		final DataTable dataArray = nodeModel.getOrigData();
		indices = new int[visibleColumns.size()];
		selectedIndices = new int[selectedColumns.size()];
		int i = 0;
		for (final String column : visibleColumns) {
			indices[i++] = dataArray.getDataTableSpec().findColumnIndex(column);
		}
		i = 0;
		for (final String selected : selectedColumns) {
			final int idx = dataArray.getDataTableSpec().findColumnIndex(
					selected);
			selectedIndices[i] = -1;
			for (int j = indices.length; j-- > 0;) {
				if (indices[j] == idx) {
					selectedIndices[i++] = j;
					break;
				}
			}
		}
	}

	@Override
	public void setRootNode(final BinaryTree<DendrogramPoint> root) {
		rootNode = root;
		super.setRootNode(root);
		if (rootNode != null) {
			keys = new String[nodeModel.getOrigData().size()];
			int i = 0;
			for (final BinaryTreeNode<DendrogramPoint> node : root
					.getNodes(Traversal.IN)) {
				if (node.isLeaf()) {
					keys[i++] = node.getContent().getRows().iterator().next()
							.getString();
				}
			}
		}
	}

	@Override
	public void paintContent(final Graphics g) {
		if (rootNode == null) {
			return;
		}
		final Stroke backupStroke = ((Graphics2D) g).getStroke();
		final Color backupColor = g.getColor();
		final List<BinaryTreeNode<DendrogramPoint>> nodes = rootNode
				.getNodes(BinaryTree.Traversal.IN);

		final FontMetrics fm = g.getFontMetrics();
		final int fontHeight = fm.getHeight();
		for (final BinaryTreeNode<DendrogramPoint> node : nodes) {
			final DendrogramPoint dendroPoint = node.getContent();
			if (dendroPoint.getRows().size() == 1 && nodeModel != null) {
				final Point point = dendroPoint.getPoint();
				leafX = point.x;
				final String key = dendroPoint.getRows().iterator().next()
						.getString();
				final int index = nodeModel.getMap().get(key).intValue();
				final DataRow row = nodeModel.getOrigData().getRow(index);
				final Color color = g.getColor();
				for (int i = 0; i < indices.length; ++i) {
					final DataCell cell = row.getCell(indices[i]);
					if (cell instanceof DoubleValue) {
						final double val = ((DoubleValue) cell)
								.getDoubleValue();
						final ColourComputer model = colourModel.getModel(
								visibleColumns.get(i), StatTypes.raw);
						final Color col = model.compute(val);
						g.setColor(col);
						g.fillRect(point.x
								+ (directionLeftToRight ? (i - visibleColumns
										.size())
										* cellWidth : i /*- 1*/
										* cellWidth), point.y - cellHeight / 2,
								cellWidth, cellHeight + 1);
						if (showValues) {
							g
									.setColor(Color.RGBtoHSB(col.getRed(), col
											.getGreen(), col.getBlue(), null)[2] > .6f ? Color.BLACK
											: Color.WHITE);
							final String str = Misc.round(val);
							g
									.drawString(
											str,
											point.x
													+ (directionLeftToRight ? (i - visibleColumns
															.size())
															* cellWidth
															: i * cellWidth)

													+ (cellWidth - fm
															.stringWidth(str))
													/ 2, point.y + fontHeight
													/ 3);
						}
					}
				}

				final ColorAttr colorAttr = nodeModel.getOrigData()
						.getDataTableSpec().getRowColor(row);
				if (colorAttr != ColorAttr.DEFAULT) {
					final Color rowColor = colorAttr.getColor();
					g.setColor(rowColor);
					g.fillRect(directionLeftToRight ? point.x
							- visibleColumns.size() * cellWidth
							- maxStringLength : leafX + visibleColumns.size()
							* cellWidth, point.y - cellHeight / 2,
							maxStringLength, cellHeight + 1);
					g
							.setColor(Color.RGBtoHSB(rowColor.getGreen(),
									rowColor.getGreen(), rowColor.getBlue(),
									null)[2] < .4f ? Color.WHITE : Color.BLACK);
				} else {
					g.setColor(Color.BLACK);
				}
				g.drawString(row.getKey().getString(),
						directionLeftToRight ? point.x - visibleColumns.size()
								* cellWidth
								- fm.stringWidth(row.getKey().getString())
								: point.x + visibleColumns.size() * cellWidth,
						point.y + /*
								 * cellHeight / 2 -
								 */fontHeight / 3);
				g.setColor(color);
			}
			// set the correct stroke and color
			g.setColor(ColorAttr.DEFAULT.getColor(node.getContent()
					.isSelected(), node.getContent().isHilite()));
			if (node.getContent().isSelected() || node.getContent().isHilite()) {
				((Graphics2D) g).setStroke(new BasicStroke(
						(lineThickness * HeatmapDendrogramDrawingPane.BOLD)));
				if (node.getContent().getRows().size() == 1) {
					final Point point = node.getContent().getPoint();
					g.drawRect(point.x
							- (directionLeftToRight ? visibleColumns.size()
									* cellWidth : 0), point.y - cellHeight / 2
							+ 1, cellWidth * visibleColumns.size(), cellHeight);
				}
			} else {
				((Graphics2D) g).setStroke(new BasicStroke(lineThickness));
			}
			if (node.getLeftChild() != null || node.getRightChild() != null) {
				// draw vertical line
				final Point leftPoint = node.getLeftChild().getContent()
						.getPoint();
				final Point rightPoint = node.getRightChild().getContent()
						.getPoint();
				g.drawLine(node.getContent().getPoint().x/* leftPoint.x */,
						leftPoint.y /* node.getContent().getPoint().y */, node
								.getContent().getPoint().x/* rightPoint.x */,
						rightPoint.y /* node.getContent().getPoint().y */);
			}
			// draw horizontal line
			if (node.getParent() != null) {
				g.setColor(ColorAttr.DEFAULT.getColor(node.getParent()
						.getContent().isSelected(), node.getParent()
						.getContent().isHilite()));
				// check if parent is selected
				// if yes bold line, else normal line
				if (node.getParent().getContent().isSelected()
						|| node.getParent().getContent().isHilite()) {
					((Graphics2D) g)
							.setStroke(new BasicStroke(
									(lineThickness * HeatmapDendrogramDrawingPane.BOLD)));
				} else {
					((Graphics2D) g).setStroke(new BasicStroke(lineThickness));
				}
				g.drawLine(node.getContent().getPoint().x, node.getContent()
						.getPoint().y,
						node.getParent().getContent().getPoint().x, node
								.getContent().getPoint().y);
			}
			((Graphics2D) g).setStroke(backupStroke);
			g.setColor(ColorAttr.SELECTED);
			for (final int selectedIndex : selectedIndices) {
				final int pos = leafX
						+ (directionLeftToRight ? (selectedIndex - visibleColumns
								.size())
								* cellWidth
								: selectedIndex * cellWidth);
				g.drawLine(pos, 0, pos, getHeight());
				g.drawLine(pos + cellWidth, 0, pos + cellWidth, getHeight());
			}
			g.setColor(backupColor);
		}
	}

	/**
	 * Sets the actual cell height of the heatmap.
	 * 
	 * @param cellHeight
	 *            The new cell height.
	 */
	public void setHeatmapCellHeight(final int cellHeight) {
		this.cellHeight = cellHeight;
	}

	/**
	 * @param colourModel
	 *            The new ColourModel.
	 */
	public void setColourModel(final ColourModel colourModel) {
		this.colourModel = colourModel;
	}

	@Override
	public String getToolTipText(final MouseEvent event) {
		if (rootNode == null) {
			return "";
		}
		final Point p = event.getPoint();
		final DataArray dataArray = nodeModel.getOrigData();
		final int allCount = visibleColumns.size();
		final int startPos = directionLeftToRight ? maxStringLength + allCount
				* cellWidth : leafX;
		if (directionLeftToRight && p.x < startPos || !directionLeftToRight
				&& p.x > startPos && p.y < getHeight()) {
			final int idx = directionLeftToRight ? allCount
					- (startPos - p.x + cellWidth - 1) / cellWidth
					: (p.x - startPos) / cellWidth;
			final int rowIdx = dataArray.size() - 1 - p.y * dataArray.size()
					/ getHeight();
			if (idx < 0 && directionLeftToRight || !directionLeftToRight
					&& idx >= allCount) {
				return keys[rowIdx];
			}
			final DataCell cell = nodeModel.getOrigData().getRow(
					nodeModel.getMap().get(keys[rowIdx])).getCell(indices[idx]);
			return "<html>"
					+ visibleColumns.get(idx)
					+ ": <b>"
					+ (cell instanceof DoubleValue ? Math
							.round(((DoubleValue) cell).getDoubleValue() * 1000) / 1000.0
							: Double.NaN) + "</b> (" + keys[rowIdx]
					+ ")</html>";
		} else {
			return super.getToolTipText(event);
		}
	}

	/**
	 * @return The width of a cell in the heatmap part.
	 */
	public int getCellWidth() {
		return cellWidth;
	}

	/**
	 * @param cellWidth
	 *            The new cell width in the heatmap part.
	 */
	public void setCellWidth(final int cellWidth) {
		this.cellWidth = cellWidth;
	}

	/**
	 * @return The (unmodifiable) {@link List} of visible columns.
	 */
	List<String> getVisibleColumns() {
		return Collections.unmodifiableList(visibleColumns);
	}

	@Override
	public void setLineThickness(final int thickness) {
		super.setLineThickness(thickness);
		lineThickness = thickness;
	}

	/**
	 * @param directionLeftToRight
	 *            The new value of indicator of left to right increase field.
	 */
	public void setHorizontalDirection(final boolean directionLeftToRight) {
		this.directionLeftToRight = directionLeftToRight;
	}

	//
	// /**
	// * @param directionUpToDown
	// * The new value of the up to down order of values field.
	// */
	// public void setVerticalDirection(final boolean directionUpToDown) {
	// this.directionUpToDown = directionUpToDown;
	// }

	/**
	 * Selects the visible columns (and clears selection of columns).
	 * 
	 * @param visibleColumns
	 *            The visible columns.
	 */
	protected void setVisibleColumns(final List<String> visibleColumns) {
		this.visibleColumns.clear();
		this.visibleColumns.addAll(visibleColumns);
		selectedColumns.clear();
		computeIndices();
	}

	/**
	 * @param selectedColumns
	 *            The selected columns.
	 */
	protected void setSelectedColumns(final List<String> selectedColumns) {
		this.selectedColumns.clear();
		this.selectedColumns.addAll(selectedColumns);
		computeIndices();
	}

	/**
	 * Updates the showValues property.
	 * 
	 * @param showValues
	 *            The new value for the property.
	 */
	public void setShowValues(final boolean showValues) {
		this.showValues = showValues;
	}
}
