/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.common.Format;
import ie.tcd.imm.hits.knime.util.ModelBuilder;
import ie.tcd.imm.hits.knime.util.VisualUtils;
import ie.tcd.imm.hits.knime.view.heatmap.ColourSelector.ColourModel;
import ie.tcd.imm.hits.knime.view.heatmap.ColourSelector.DoubleValueSelector.Model;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.ArrangementModel;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeView.VolatileModel;
import ie.tcd.imm.hits.knime.view.heatmap.SliderModel.Type;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.util.Pair;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.swing.JComponent;

import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * Shows a heatmap of values.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class Heatmap extends JComponent implements HiLiteListener {
	private static final long serialVersionUID = 7832090816939923780L;
	private static final Integer ZERO = Integer.valueOf(0);
	private ViewModel viewModel;
	private final WellViewPanel[] wells = new WellViewPanel[384];
	private Map<String, Pair<Integer, Integer>> keyToPlateAndPosition;
	private VolatileModel volatileModel;
	/** From 0. */
	private int plate;

	/**
	 * Constructs a {@link Heatmap} with the {@code viewModel} and
	 * {@code dataModel}.
	 * 
	 * @param viewModel
	 *            The {@link ViewModel} to constrain the well layout.
	 * @param dataModel
	 *            The {@link HeatmapNodeModel} to set the initial values of the
	 *            wells.
	 */
	public Heatmap(final ViewModel viewModel, final HeatmapNodeModel dataModel) {
		super();
		this.viewModel = viewModel;
		internalUpdateViewModel();
	}

	// @Override
	// protected void paintComponent(final Graphics g) {
	// super.paintComponent(g);
	// final Rectangle bounds = getBounds();
	// final int rowCount = viewModel.getFormat().getRow();
	// final int colCount = viewModel.getFormat().getCol();
	// final int radius = Math.min(bounds.height / rowCount, bounds.width
	// / colCount);
	// final Random random = new Random(12);
	// for (int i = 0; i < rowCount; ++i) {
	// for (int j = 0; j < colCount; ++j) {
	// g.setColor(new Color(random.nextInt(), false));
	// g.fillOval(bounds.x + j * radius, bounds.y + i * radius,
	// radius, radius);
	// }
	// }
	// }

	/**
	 * Updates the values of the {@link Heatmap} using the parameters.
	 * 
	 * @param nodeModel
	 *            This {@link HeatmapNodeModel} should contain all values for
	 *            the experiment.
	 * @param volatileModel
	 *            This contains the current values of the layout.
	 * @param row
	 *            We want to see the {@link Heatmap} from this row.
	 * @param col
	 *            We want to see the {@link Heatmap} from this column.
	 */
	public void setModel(final HeatmapNodeModel nodeModel,
			final VolatileModel volatileModel, final int row, final int col) {
		// this.volatileModel.removeActionListener(this);
		this.volatileModel = volatileModel;
		// volatileModel.addActionListener(this);
		keyToPlateAndPosition = nodeModel.getModelBuilder()
				.getKeyToPlateAndPosition();
		setHilites();
		replicates(nodeModel);
		repaint();
	}

	private void setHilites() {
		final Map<SliderModel, Integer> sliderPositions = volatileModel
				.getSliderPositions();
		final Collection<SliderModel> sliders = viewModel.getMain()
				.getArrangementModel().getSliders().get(Type.Selector);
		final int currentPlate = (sliders.size() > 0 ? /*
														 * sliderPositions.get(
														 * sliders.iterator().next()).intValue()
														 */sliders.iterator().next().getSelections().iterator().next() : 1) - 1;
		final boolean[] hiliteValues = volatileModel
				.getHiliteValues(currentPlate);
		final boolean[] selections = volatileModel
				.getSelectionValues(currentPlate);
		for (int i = viewModel.getFormat().getCol()
				* viewModel.getFormat().getRow(); i-- > 0;) {
			wells[i].setHilited(hiliteValues[i]);
			wells[i].setSelected(selections[i]);
		}
	}

	private void replicates(final HeatmapNodeModel nodeModel) {
		final int cols = viewModel.getFormat().getCol();
		final int rows = viewModel.getFormat().getRow();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected plate values. */
		final Map<Integer, List<Integer>> platePos = new HashMap<Integer, List<Integer>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected experiment values. */
		final Map<Integer, List<String>> experimentPos = new HashMap<Integer, List<String>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected normalisation values. */
		final Map<Integer, List<String>> normalisationPos = new HashMap<Integer, List<String>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected parameter values. */
		final Map<Integer, List<String>> parameterPos = new HashMap<Integer, List<String>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected statistics values. */
		final Map<Integer, List<StatTypes>> statPos = new HashMap<Integer, List<StatTypes>>();
		/* 0-MAX_INDEPENDENT_FACTORS -> selected replicate values. */
		final Map<Integer, List<Integer>> replicatePos = new HashMap<Integer, List<Integer>>();
		for (int i = SliderModel.MAX_INDEPENDENT_FACTORS; i-- > 0;) {
			platePos.put(Integer.valueOf(i), new ArrayList<Integer>());
			experimentPos.put(Integer.valueOf(i), new ArrayList<String>());
			normalisationPos.put(Integer.valueOf(i), new ArrayList<String>());
			parameterPos.put(Integer.valueOf(i), new ArrayList<String>());
			statPos.put(Integer.valueOf(i), new ArrayList<StatTypes>());
			replicatePos.put(Integer.valueOf(i), new ArrayList<Integer>());
		}
		SliderModel experimentSlider = null;
		SliderModel normalisationSlider = null;
		SliderModel parameterSlider = null;
		SliderModel statSlider = null;
		SliderModel plateSlider = null;
		SliderModel replicateSlider = null;
		for (final SliderModel slider : viewModel.getMain()
				.getArrangementModel().getSliderModels()) {

			for (final ParameterModel model : slider.getParameters()) {
				switch (model.getType()) {
				case plate:
					selectionToPos(platePos, slider, Integer.class);
					plateSlider = slider;
					break;
				case experimentName:
					selectionToPos(experimentPos, slider, String.class);
					experimentSlider = slider;
					break;
				case normalisation:
					selectionToPos(normalisationPos, slider, String.class);
					normalisationSlider = slider;
					break;
				case parameter:
					selectionToPos(parameterPos, slider, String.class);
					parameterSlider = slider;
					break;
				case metaStatType: {
					selectionToPos(statPos, slider, StatTypes.class);
					statSlider = slider;
					break;
				}
				case replicate: {
					selectionToPos(replicatePos, slider, Integer.class);
					replicateSlider = slider;
					break;
				}

				default:
					// Do nothing.
					break;
				}
			}
		}
		final LinkedHashMap<ParameterModel, Collection<SliderModel>> mainArrangement = viewModel
				.getMain().getArrangementModel().getMainArrangement();
		// final SliderModel aPlateSlider = ArrangementModel.selectNth(
		// mainArrangement, 0, StatTypes.plate);
		final SliderModel firstParamSlider = findSlider(viewModel.getMain()
				.getArrangementModel().getSliderModels(), viewModel.getMain()
				.getPrimerParameters());
		final SliderModel secondParamSlider = findSlider(viewModel.getMain()
				.getArrangementModel().getSliderModels(), viewModel.getMain()
				.getSeconderParameters());
		final int size = computeSplitterCount(Arrays.asList(firstParamSlider,
				secondParamSlider));
		plate = plateSlider.getSelections().size() == 1 ? ((Number) plateSlider
				.getValueMapping().get(
						plateSlider.getSelections().iterator().next())
				.getRight()).intValue() - 1 : -1;// volatileModel.getSliderPositions().get(aPlateSlider).intValue()
		// - 1;
		// replicateMap.entrySet().iterator().next()
		// .getValue().keySet().size();//
		// nodeModel.scoreValues.get(platePos[0]).entrySet().size()
		// .next().getColorLegend().size();
		final Color[][] colors = new Color[rows * cols][size/* * replicateCount */];
		// for (final Entry<ParameterModel, Collection<SliderModel>> mainEntry :
		// mainArrangement
		// .entrySet()) {

		// final int firstParamCount = computeSplitterCount(Collections
		// .singleton(firstParamSlider));
		final ColourModel colourModel = volatileModel.getColourModel();
		final ModelBuilder modelBuilder = nodeModel.getModelBuilder();
		final Map<String, Map<String, Map<Integer, Map<String, Map<StatTypes, double[]>>>>> scores = modelBuilder
				.getScores();
		// ENH use BDB
		findColourValues(Format._96, colors, scores, colourModel,
				firstParamSlider, null, secondParamSlider, null, Arrays.asList(
						new Pair<SliderModel, List<?>>(experimentSlider,
								experimentPos.get(ZERO)),
						new Pair<SliderModel, List<?>>(normalisationSlider,
								normalisationPos.get(ZERO)),
						new Pair<SliderModel, List<?>>(plateSlider, platePos
								.get(ZERO)), new Pair<SliderModel, List<?>>(
								parameterSlider, parameterPos.get(ZERO)),
						new Pair<SliderModel, List<?>>(statSlider, statPos
								.get(ZERO))));
		final Map<String, Map<String, Map<Integer, Map<Integer, Map<String, Map<StatTypes, double[]>>>>>> replicates = modelBuilder
				.getReplicates();
		findColourValues(Format._96, colors, scores, colourModel,
				firstParamSlider, null, secondParamSlider, null, Arrays.asList(
						new Pair<SliderModel, List<?>>(experimentSlider,
								experimentPos.get(ZERO)),
						new Pair<SliderModel, List<?>>(normalisationSlider,
								normalisationPos.get(ZERO)),
						new Pair<SliderModel, List<?>>(plateSlider, platePos
								.get(ZERO)), new Pair<SliderModel, List<?>>(
								replicateSlider, replicatePos.get(ZERO)),
						new Pair<SliderModel, List<?>>(parameterSlider,
								parameterPos.get(ZERO)),
						new Pair<SliderModel, List<?>>(statSlider, statPos
								.get(ZERO))));
		// exp, norm, plate, col
		final Map<String, Map<String, Map<Integer, Color[]>>> colours = modelBuilder
				.getColours();
		assert colours != null;
		// norm, plate, col
		final Map<String, Map<Integer, Color[]>> normColors = colours == null ? null
				: experimentPos.get(ZERO).size() > 0 ? colours
						.get(experimentPos.get(ZERO).get(0)) : null;
		if (normColors != null) {
			final Map<Integer, Color[]> plateColours = normalisationPos.get(
					ZERO).size() > 0 ? normColors.get(normalisationPos
					.get(ZERO).get(0)) : null;
			if (plateColours != null) {
				final Color[] array = plateColours.get(Integer
						.valueOf(plate + 1));
				if (array != null) {
					for (int i = rows * cols; i-- > 0;) {
						final Color color = array[i];
						wells[i]
								.setBackground(color == null ? ColorAttr.BACKGROUND
										: color);
					}
				}
			}
		}
		for (int i = rows * cols; i-- > 0;) {
			wells[i].setColors(colors[i]);
		}
		for (int i = rows * cols; i-- > 0;) {
			final String l = InfoParser.parse(experimentPos.get(ZERO),
					normalisationPos.get(ZERO), viewModel.getLabelPattern(), /*
																				 * plate +
																				 * 1
																				 */
					platePos.get(ZERO), i / 12, i % 12, nodeModel);
			wells[i].setLabels(l);
		}
	}

	/**
	 * @param colors
	 * @param scores
	 * @param colourModel
	 * @param firstParamSlider
	 * @param secondParamSlider
	 * @param pairs
	 */
	private void findColourValues(final Format format, final Color[][] colors,
			final Object scores, final ColourModel colourModel,
			final SliderModel firstParamSlider, final Integer firstSelection,
			final SliderModel secondParamSlider, final Integer secondSelection,
			final List<Pair<SliderModel, List<?>>> pairs) {
		final int firstParamCount = firstParamSlider == null ? 1
				: firstParamSlider.getSelections().size();
		double[] array = null;
		if (pairs.size() > 0) {
			Map<?, ?> map = (Map<?, ?>) scores;
			int i = 0;
			for (final Iterator<Pair<SliderModel, List<?>>> it = pairs
					.iterator(); it.hasNext(); ++i) {
				final Pair<SliderModel, List<?>> pair = it.next();
				final SliderModel sliderModel = pair.getLeft();
				final List<?> selectedValues = pair.getRight();
				if (map == null) {
					return;
				}
				if (sliderModel == firstParamSlider) {
					for (final Integer selected : sliderModel.getSelections()) {
						findColourValues(format, colors, map.get(sliderModel
								.getValueMapping().get(selected).getRight()),
								colourModel, firstParamSlider, selected,
								secondParamSlider, secondSelection, pairs
										.subList(i + 1, pairs.size()));
					}
					return;
				}
				if (sliderModel == secondParamSlider) {
					for (final Integer selected : sliderModel.getSelections()) {
						findColourValues(format, colors, map.get(sliderModel
								.getValueMapping().get(selected).getRight()),
								colourModel, firstParamSlider, firstSelection,
								secondParamSlider, selected, pairs.subList(
										i + 1, pairs.size()));
					}
					return;
				} else {
					assert sliderModel.getSelections().size() == 1;
					final Object obj = map.get(sliderModel.getValueMapping()
							.get(sliderModel.getSelections().iterator().next())
							.getRight());
					if (it.hasNext()) {
						map = (Map<?, ?>) obj;
					} else {
						array = (double[]) obj;
					}
				}
			}
		}
		final int fp = firstSelection == null ? 0
				: firstSelection.intValue() - 1;
		final int sp = secondSelection == null ? 0
				: secondSelection.intValue() - 1;
		final Model model = ColourSelector.DEFAULT_MODEL;// colourModel.getModel("Cell
		// Elongation",
		// StatTypes.score);
		if (array != null) {
			for (int p = format.getCol() * format.getRow(); p-- > 0;) {
				colors[p][sp * firstParamCount + fp] = VisualUtils
						.colourOf(array[p], model.getDown(), model
								.getMiddleVal() == null ? null : model
								.getMiddle(), model.getUp(),
								model.getDownVal(),
								model.getMiddleVal() == null ? 0.0 : model
										.getMiddleVal().doubleValue(), model
										.getUpVal());

			}
		}
	}

	/**
	 * @param sliderModels
	 * @param parameters
	 * @return
	 */
	private @Nullable
	SliderModel findSlider(final Set<SliderModel> sliderModels,
			final List<ParameterModel> parameters) {
		if (parameters.size() == 0) {
			return null;
		}
		for (final SliderModel sliderModel : sliderModels) {
			if (sliderModel.getParameters().iterator().next().equals(
					parameters.get(0))) {
				return sliderModel;
			}
		}
		return null;
	}

	/**
	 * @param <T>
	 * @param posMap
	 * @param slider
	 * @param cls
	 */
	private <T> void selectionToPos(final Map<Integer, List<T>> posMap,
			final SliderModel slider, final Class<? extends T> cls) {
		final List<T> list = posMap.get(Integer.valueOf(slider.getSubId()));
		for (final Integer selection : slider.getSelections()) {
			list.add(cls.cast(slider.getValueMapping().get(selection)
					.getRight()));
		}
	}

	@Deprecated
	private void replicatesOld(final HeatmapNodeModel nodeModel) {
		final int cols = viewModel.getFormat().getCol();
		final int rows = viewModel.getFormat().getRow();
		final Map<SliderModel, Integer> sliderPositions = volatileModel
				.getSliderPositions();
		final int[] platePos = new int[SliderModel.MAX_INDEPENDENT_FACTORS];
		final String[] experimentPos = new String[SliderModel.MAX_INDEPENDENT_FACTORS];
		final String[] normalisationPos = new String[SliderModel.MAX_INDEPENDENT_FACTORS];
		// for (final Entry<SliderModel, Integer> entry : sliderPositions
		// .entrySet()) {
		// final SliderModel slider = entry.getKey();
		for (final Entry<Type, Collection<SliderModel>> entry : viewModel
				.getMain().getArrangementModel().getSliders().entrySet()) {
			for (final SliderModel slider : entry.getValue()) {

				for (final ParameterModel model : slider.getParameters()) {
					switch (model.getType()) {
					case plate:
						platePos[slider.getSubId()] = slider.getSelections()
								.iterator().next().intValue();// entry.getValue().intValue();
						break;
					case experimentName:
						experimentPos[slider.getSubId()] = (String) slider
								.getValueMapping().get(
										slider.getSelections().iterator()
												.next()/* entry.getValue() */)
								.getRight();
						break;
					case normalisation:
						normalisationPos[slider.getSubId()] = (String) slider
								.getValueMapping().get(
										slider.getSelections().iterator()
												.next()/* entry.getValue() */)
								.getRight();
						break;
					default:
						// Do nothing.
						break;
					}
				}
			}
		}
		final String experiment;
		final String normalisation;
		final LinkedHashMap<ParameterModel, Collection<SliderModel>> mainArrangement = viewModel
				.getMain().getArrangementModel().getMainArrangement();
		final SliderModel aPlateSlider = ArrangementModel.selectNth(
				mainArrangement, 0, StatTypes.plate);
		final Collection<SliderModel> splitterSliders = viewModel.getMain()
				.getArrangementModel().getSliders().get(Type.Splitter);
		assert splitterSliders != null;
		assert splitterSliders.size() > 0;
		int size = 0;
		for (final Entry<ParameterModel, Collection<SliderModel>> entry : mainArrangement
				.entrySet()) {
			if (entry.getKey().getAggregateType() != null) {
				size += computeContributedValuesCount(entry.getValue());
			} else {
				size += computeSplitterCount(splitterSliders);
			}
		}
		plate = aPlateSlider.getSelections().iterator().next().intValue() - 1;// volatileModel.getSliderPositions().get(aPlateSlider).intValue()
		// - 1;
		// replicateMap.entrySet().iterator().next()
		// .getValue().keySet().size();//
		// nodeModel.scoreValues.get(platePos[0]).entrySet().size()
		final SliderModel replicateSlider = ArrangementModel.selectNth(
				mainArrangement, 0, StatTypes.replicate);
		final int replicateCount = replicateSlider.getSelections().size();// getParameters().iterator()
		// .next().getColorLegend().size();
		final Color[][] colors = new Color[rows * cols][size/* * replicateCount */];
		for (final Entry<ParameterModel, Collection<SliderModel>> mainEntry : mainArrangement
				.entrySet()) {
			final Collection<SliderModel> currentSliders = mainEntry.getValue();
			String selectedParameter = null;
			SliderModel parameterSlider = null;
			StatTypes selected = null;// StatTypes.normalized;
			SliderModel statSlider = null;
			Integer platePosition = null;
			SliderModel plateSlider = null;
			for (final SliderModel currentSlider : currentSliders) {
				final List<ParameterModel> parameters = currentSlider
						.getParameters();
				for (final ParameterModel parameterModel : parameters) {
					final Integer currentSliderValue = currentSlider
							.getSelections().size() != 1 ? null : currentSlider
							.getSelections().iterator().next();// sliderPositions
					// .get(currentSlider);
					// FIXME if problem with null
					switch (parameterModel.getType()) {
					case metaStatType: {
						assert parameters.size() == 1;
						final Pair<ParameterModel, Object> value = currentSlider
								.getValueMapping().get(currentSliderValue);
						assert value != null;
						final Object object = value.getRight();
						assert object instanceof StatTypes : object.getClass();
						selected = currentSlider.getType() == Type.Splitter ? null
								: (StatTypes) object;
						statSlider = currentSlider;
						break;
					}
					case parameter: {
						assert parameters.size() == 1;
						if (currentSliderValue != null) {
							final Pair<ParameterModel, Object> value = currentSlider
									.getValueMapping().get(currentSliderValue);
							assert value != null;
							final Object object = value.getRight();
							assert object instanceof String : object.getClass();
							selectedParameter = currentSlider.getType() == Type.Splitter ? null
									: (String) object;
						}
						parameterSlider = currentSlider;
						break;
					}
					case plate: {
						assert parameters.size() == 1;
						final Pair<ParameterModel, Object> value = currentSlider
								.getValueMapping().get(currentSliderValue);
						assert value != null;
						final Object object = value.getRight();
						assert object instanceof Integer : object.getClass();
						platePosition = currentSlider.getType() == Type.Splitter ? null
								: (Integer) object;
						plateSlider = currentSlider;
						break;
					}
					default:// Not handled yet.
						break;
					}
				}
			}
			final int paramCount = computeSplitterCount(Collections
					.singleton(parameterSlider));
			final Set<String> currentParameters = new HashSet<String>();
			for (final Entry<Integer, Pair<ParameterModel, Object>> m : parameterSlider
					.getValueMapping().entrySet()) {
				if (parameterSlider.getSelections().contains(m.getKey())) {
					currentParameters.add((String) m.getValue().getRight());
				}
			}
			final Set<Integer> currentReplicates = new HashSet<Integer>();
			for (final Entry<Integer, Pair<ParameterModel, Object>> m : replicateSlider
					.getValueMapping().entrySet()) {
				if (replicateSlider.getSelections().contains(m.getKey())) {
					currentReplicates.add((Integer) m.getValue().getRight());
				}
			}
			final ColourModel colourModel = volatileModel.getColourModel();
			if (selected.isUseReplicates()) {
				final Map<Integer, Map<String, Map<StatTypes, double[]>>> replicateMap = nodeModel
						.getModelBuilder().getReplicates()
						.get(experimentPos[0]).get(normalisationPos[0]).get(
								platePos[0]);
				int replicateValue = 0;
				for (final Entry<Integer, Map<String, Map<StatTypes, double[]>>> replicateEntry : replicateMap
						.entrySet()) {
					if (!currentReplicates.contains(replicateEntry.getKey())) {
						continue;
					}
					final Map<String, Map<StatTypes, double[]>> paramMap = replicateEntry
							.getValue();
					int param = 0;
					for (final Map.Entry<String, Map<StatTypes, double[]>> entry : paramMap
							.entrySet()) {
						final String parameter = entry.getKey();
						if (currentParameters.contains(parameter)) {
							Model model = null;
							if (colourModel != null) {
								model = colourModel.getModel(parameter,
										selected);
							}
							if (model == null) {
								model = ColourSelector.DEFAULT_MODEL;
							}
							for (int i = entry.getValue().get(selected).length; i-- > 0;) {
								final Color colorI = VisualUtils.colourOf(entry
										.getValue().get(selected)[i], model
										.getDown(),
										model.getMiddleVal() == null ? null
												: model.getMiddle(), model
												.getUp(), model.getDownVal(),
										model.getMiddleVal() == null ? 0.0
												: model.getMiddleVal()
														.doubleValue(), model
												.getUpVal());
								switch (viewModel.getFormat()) {
								case _96:
									colors[i][replicateValue * paramCount
											+ param] = colorI;
									break;
								case _384:
									colors[i / 12 * cols + i % 12][/*
																	 * (replicateEntry
																	 * .getKey().intValue() -
																	 * 1)
																	 */replicateValue * paramCount + param] = colorI;
								default:
									break;
								}
							}
							++param;
						}
					}
					++replicateValue;
				}
			} else {
				if (selectedParameter == null) {
					int param = 0;
					for (final Entry<Integer, Pair<ParameterModel, Object>> entry : parameterSlider
							.getValueMapping().entrySet()) {
						if (!parameterSlider.getSelections().contains(
								entry.getKey())) {
							continue;
						}
						// final int param = entry.getKey().intValue() - 1;
						final String paramName = (String) entry.getValue()
								.getRight();
						Model model = null;
						if (colourModel != null) {
							model = colourModel.getModel(paramName, selected);
						}
						if (model == null) {
							model = ColourSelector.DEFAULT_MODEL;
						}
						// TODO handle selected == null.
						// TODO handle plateposition == null
						final double[] values = nodeModel.getModelBuilder()
								.getScores().get(experimentPos[0]).get(
										normalisationPos[0]).get(platePosition)
								.get(paramName).get(selected);
						for (int i = values.length; i-- > 0;) {
							final Color colorI = VisualUtils.colourOf(
									values[i], model.getDown(), model
											.getMiddleVal() == null ? null
											: model.getMiddle(), model.getUp(),
									model.getDownVal(),
									model.getMiddleVal() == null ? 0.0 : model
											.getMiddleVal().doubleValue(),
									model.getUpVal());
							// final Color colorI = VisualUtils.colourOf(
							// values[i], Color.GREEN, Color.YELLOW,
							// Color.RED, -2, 0, 2);
							for (int replicate = replicateCount; replicate-- > 0;) {
								switch (viewModel.getFormat()) {
								case _96:
									colors[i][replicate * paramCount + param] = colorI;
									break;
								case _384:
									colors[i / 12 * cols + i % 12][replicate
											* paramCount + param] = colorI;
								default:
									break;
								}
							}
						}
						++param;
					}
				} else {
					// TODO handle platePosition == null
					final double[] values = nodeModel.getModelBuilder()
							.getScores().get(experimentPos[0]).get(
									normalisationPos[0]).get(
									Integer.valueOf(platePosition)).get(
									selectedParameter).get(selected);
					for (int i = values.length; i-- > 0;) {
						final Color colorI = VisualUtils.colourOf(values[i],
								Color.GREEN, Color.YELLOW, Color.RED, -2, 0, 2);
						for (int replicate = replicateCount; replicate-- > 0;) {
							for (int param = paramCount; param-- > 0;) {
								switch (viewModel.getFormat()) {
								case _96:
									colors[i][replicate * paramCount + param] = colorI;
									break;
								case _384:
									colors[i / 12 * cols + i % 12][replicate
											* paramCount + param] = colorI;
								default:
									break;
								}
							}
						}
					}
				}
			}
		}
		final Map<String, Map<String, Map<Integer, Color[]>>> colours = nodeModel
				.getModelBuilder().getColours();
		assert colours != null;
		final Map<String, Map<Integer, Color[]>> normColors = colours == null ? null
				: colours.get(experimentPos[0]);
		if (normColors != null) {
			final Map<Integer, Color[]> plateColours = normColors
					.get(normalisationPos[0]);
			if (plateColours != null) {
				final Color[] array = plateColours.get(Integer
						.valueOf(plate + 1));
				if (array != null) {
					for (int i = rows * cols; i-- > 0;) {
						final Color color = array[i];
						wells[i]
								.setBackground(color == null ? ColorAttr.BACKGROUND
										: color);
					}
				}
			}
		}
		for (int i = rows * cols; i-- > 0;) {
			wells[i].setColors(colors[i]);
		}
		for (int i = rows * cols; i-- > 0;) {
			final String l = InfoParser.parse(experimentPos[0],
					normalisationPos[0], viewModel.getLabelPattern(),
					plate + 1, i / 12, i % 12, nodeModel);
			wells[i].setLabels(l);
		}
	}

	private int computeSplitterCount(final Collection<SliderModel> sliders) {
		int ret = 1;
		for (final SliderModel slider : sliders) {
			// final List<ParameterModel> parameters = slider.getParameters();
			// assert parameters.size() == 1 : slider;
			// final ParameterModel parameter = parameters.iterator().next();
			if (slider != null) {
				ret *= slider.getSelections().size();// parameter.getColorLegend().size();
			}
		}
		return ret;
	}

	private int computeContributedValuesCount(
			final Collection<SliderModel> value) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Updates the {@link Heatmap} using the values from {@code viewModel}.
	 * 
	 * @param viewModel
	 *            The new arrangement of the wells.
	 */
	public void setViewModel(final ViewModel viewModel) {
		this.viewModel = viewModel;
		internalUpdateViewModel();
		repaint();
	}

	private void internalUpdateViewModel() {
		removeAll();
		final Format format = viewModel.getFormat();
		final int rows = format.getRow();
		final int cols = format.getCol();
		setLayout(new GridLayout(rows, cols));
		for (int j = 0; j < cols; ++j) {
			for (int i = 0; i < rows; ++i) {
				final WellViewPanel well = new WellViewPanel(true, viewModel, j
						* rows + i);
				well.setPreferredSize(new Dimension(getBounds().width / cols,
						getBounds().height / rows));
				well.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(final MouseEvent e) {
						super.mouseClicked(e);
						final WellViewPanel source = (WellViewPanel) e
								.getSource();
						final boolean oldSelection = source.isSelected();
						if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0) {
							volatileModel.clearSelection();
							for (final WellViewPanel well : wells) {
								if (well != null) {
									well.setSelected(false);
								}
							}
						}
						source.setSelected(!oldSelection);
						volatileModel.setSelection(plate, source
								.getPositionOnPlate(), source.isSelected());
						repaint();
					}
				});
				wells[j * rows + i] = well;
				add(well);
			}
		}
		validate();
	}

	@Override
	public void hiLite(final KeyEvent event) {
		hilite(event, true);
	}

	private void hilite(final KeyEvent event, final boolean hilite) {
		for (final RowKey key : event.keys()) {
			final Pair<Integer, Integer> pair = keyToPlateAndPosition.get(key
					.getString());
			if (pair != null) {
				assert pair != null;
				final int plate = pair.getLeft().intValue() - 1;
				final int pos = pair.getRight().intValue();
				volatileModel.setHilite(plate, pos, hilite);
			}
		}
		setHilites();
	}

	@Override
	public void unHiLite(final KeyEvent event) {
		hilite(event, false);
	}

	@Override
	public void unHiLiteAll(final KeyEvent event) {
		for (final WellViewPanel well : wells) {
			if (well != null) {
				well.setHilited(false);
			}
		}
		volatileModel.unHiliteAll();
	}

	/**
	 * This method adds a {@link MouseListener} for each well of the
	 * {@link Heatmap}.
	 * 
	 * @param listener
	 *            A non-{@code null} {@link MouseListener}.
	 */
	public void addClickListenerForEveryWell(final MouseListener listener) {
		for (final WellViewPanel well : wells) {
			if (well != null) {
				well.addMouseListener(listener);
			}
		}
	}
}
