package org.meveocrm.admin.action.reporting;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.action.BaseBean;
import org.meveo.model.dwh.BarChart;
import org.meveo.model.dwh.Chart;
import org.meveo.model.dwh.LineChart;
import org.meveo.model.dwh.MeasurableQuantity;
import org.meveo.model.dwh.MeasuredValue;
import org.meveo.model.dwh.MeasurementPeriodEnum;
import org.meveo.model.dwh.PieChart;
import org.meveo.service.base.local.IPersistenceService;
import org.meveocrm.services.dwh.ChartService;
import org.meveocrm.services.dwh.MeasurableQuantityService;
import org.meveocrm.services.dwh.MeasuredValueService;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.PieChartModel;

@Dependent
public class ChartEntityBean<T extends Chart, CM extends ChartModel, EM extends ChartEntityModel<T, CM>> extends BaseBean<T> {

	@Inject
	protected ChartService<T> chartService;

	@Inject
	protected MeasuredValueService mvService;

	@Inject
	protected MeasurableQuantityService mqService;

	protected EM chartEntityModel;

	protected List<EM> chartEntityModels = new ArrayList<EM>();

	private static final long serialVersionUID = 5241132812597358412L;

	public ChartEntityBean() {
		super();
	}

	public ChartEntityBean(Class<T> clazz) {
		super(clazz);
	}

	@Override
	protected IPersistenceService<T> getPersistenceService() {
		return chartService;
	}

	@Override
	protected String getListViewName() {
		return "charts";
	}

	@SuppressWarnings("unchecked")
	public EM buildChargeEntityModel(Date fromDate, Date toDate, MeasurableQuantity mq, T chart, String dimension1, String dimension2, String dimension3, String dimension4) {
		EM result = null;
		// log.debug(
		// "buildChargeEntityModel {} from {} to {}, dimension1={}, dimension2={},"
		// + " dimension3={}, dimension4={}",
		// mq.getCode(), fromDate, toDate, dimension1, dimension2, dimension3,
		// dimension4);
		List<MeasuredValue> mvs = mvService.getByDateAndPeriod(mq.getCode(), fromDate, toDate, mq.getMeasurementPeriod(), mq);

		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
		ChartSeries mvSeries = new ChartSeries();

		mvSeries.setLabel(sdf.format(fromDate.getTime()));
		boolean empty = mvs.size() == 0;
		if (!empty) {
			empty = true;
			Map<String, BigDecimal> aggregatedValues = new HashMap<String, BigDecimal>();
			Map<String, List<String>> aggregatedCount = new HashMap<String, List<String>>();
			boolean additive = mq.isAdditive();
			for (MeasuredValue measuredValue : mvs) {
				String key = sdf.format(measuredValue.getDate());
				// log.debug("md key={}, aggVal={}, dim1={},dim2={},dim3={},dim4={},",
				// key, aggregatedValues.get(key),
				// measuredValue.getDimension1(), measuredValue.getDimension2(),
				// measuredValue.getDimension3(),
				// measuredValue.getDimension4());
				if (measuredValue.getValue() != null && (StringUtils.isBlank(dimension1) || dimension1.equals(measuredValue.getDimension1()))
						&& (StringUtils.isBlank(dimension2) || dimension2.equals(measuredValue.getDimension2()))
						&& (StringUtils.isBlank(dimension3) || dimension3.equals(measuredValue.getDimension3()))
						&& (StringUtils.isBlank(dimension4) || dimension4.equals(measuredValue.getDimension4()))) {
					if (aggregatedValues.containsKey(key) && aggregatedValues.get(key) != null) {
						aggregatedValues.put(key, aggregatedValues.get(key).add(measuredValue.getValue()));
						if (!additive) {
							String aggregationkey = StringUtils.isBlank(dimension1) ? "" : dimension1;
							if (!aggregatedCount.get(key).contains(aggregationkey)) {
								aggregatedCount.get(key).add(aggregationkey);
							}
						}
						// log.debug("md key={}, aggVal>{}", key,
						// aggregatedValues.get(key));
					} else {
						aggregatedValues.put(key, measuredValue.getValue());
						if (!additive) {
							String aggregationkey = StringUtils.isBlank(dimension1) ? "" : dimension1;
							List<String> val = new ArrayList<>();
							val.add(aggregationkey);
							aggregatedCount.put(key, val);
						}
						// log.debug("md key={}, aggVal={}", key,
						// aggregatedValues.get(key));
					}
					if (empty) {
						empty = false;
					}
				}
			}
			List<String> keyList = new ArrayList<String>(aggregatedValues.keySet());
			Collections.sort(keyList);
			Collections.reverseOrder();
			for (String key : keyList) {
				mvSeries.set(key, additive ? aggregatedValues.get(key) : (aggregatedValues.get(key).divide(new BigDecimal(aggregatedCount.get(key).size()))));
			}
		}
		if (empty) {
			mvSeries.set("NO RECORDS", 0);
			log.info("No measured values found for : " + mq.getCode());
		}
		boolean isAdmin = currentUser.hasRole("administrateur");
		boolean equalUser = chart.getAuditable().isCreator(currentUser) ;
        boolean sameRoleWithChart = chart.getRole() != null && currentUser.hasRole(chart.getRole().getDescription());
		chart.setVisible(isAdmin || equalUser || sameRoleWithChart);
		if (chart instanceof BarChart) {
			BarChartModel chartModel = new BarChartModel();
			chartModel.addSeries(mvSeries);
			chartModel.setTitle(mq.getDescription());
			configureBarChartModel(chartModel, (BarChart) chart);
			BarChartEntityModel chartEntityModel = new BarChartEntityModel();
			chartEntityModel.setModel(chartModel);
			chartEntityModel.setChart((BarChart) chart);
			result = (EM) chartEntityModel;
		} else if (chart instanceof LineChart) {
			LineChartModel chartModel = new LineChartModel();
			chartModel.addSeries(mvSeries);
			chartModel.setTitle(mq.getDescription());
			configureLineChartModel(chartModel, (LineChart) chart, mvSeries);
			LineChartEntityModel chartEntityModel = new LineChartEntityModel();
			chartEntityModel.setModel(chartModel);
			chartEntityModel.setChart((LineChart) chart);
			result = (EM) chartEntityModel;
		} else if (chart instanceof PieChart) {
			PieChartModel chartModel = new PieChartModel();
			chartModel.setTitle(mq.getDescription());
			configurePieChartModel(chartModel, (PieChart) chart, mvSeries);
			PieChartEntityModel chartEntityModel = new PieChartEntityModel();
			chartEntityModel.setModel(chartModel);
			chartEntityModel.setChart((PieChart) chart);
			result = (EM) chartEntityModel;
		}
		if (!empty) {
			result.setDimension1List(mvService.getDimensionList(1, fromDate, toDate, mq));
			result.setDimension2List(mvService.getDimensionList(2, fromDate, toDate, mq));
			result.setDimension3List(mvService.getDimensionList(3, fromDate, toDate, mq));
			result.setDimension4List(mvService.getDimensionList(4, fromDate, toDate, mq));
		}
		return result;
	}

	public List<EM> initChartModelList() {
		log.debug("initChartModelList");
		Calendar fromDate = Calendar.getInstance();
		fromDate.set(Calendar.DAY_OF_MONTH, 1);
		Calendar toDate = Calendar.getInstance();
		toDate.setTime(fromDate.getTime());
		toDate.add(Calendar.MONTH, 1);

		chartEntityModels = new ArrayList<EM>();
		List<T> chartList = chartService.list();

		for (T chart : chartList) {
			MeasurableQuantity mq = chart.getMeasurableQuantity();
			EM chartEntityModel = buildChargeEntityModel(fromDate.getTime(), toDate.getTime(), mq, chart, null, null, null, null);
			chartEntityModels.add(chartEntityModel);
			log.debug("add model {}", mq.getCode());
		}
		return chartEntityModels;
	}

	public EM getChartEntityModel() {
		log.debug("getChartEntityModel");
		if (chartEntityModel == null) {
			if (entity != null && entity.getMeasurableQuantity() != null) {
				Calendar fromDate = Calendar.getInstance();
				fromDate.set(Calendar.DAY_OF_MONTH, 1);
				Calendar toDate = Calendar.getInstance();
				toDate.setTime(fromDate.getTime());
				toDate.add(Calendar.MONTH, 1);
				MeasurableQuantity mq = getEntity().getMeasurableQuantity();
				chartEntityModel = buildChargeEntityModel(fromDate.getTime(), toDate.getTime(), mq, entity, null, null, null, null);
			}
		}
		return chartEntityModel;
	}

	public void setChartEntityModel(EM chartEntityModel) {
		this.chartEntityModel = chartEntityModel;
	}

	public void setModel(Integer modelIndex) {
		log.debug("setModel index={}", modelIndex);
		EM curr = chartEntityModels.get(modelIndex);
		MeasurableQuantity mq = curr.getChart().getMeasurableQuantity();
		curr.setModel(buildChargeEntityModel(curr.getMinDate(), curr.getMaxDate(), mq, curr.getChart(), curr.getDimension1(), curr.getDimension2(), curr.getDimension3(),
				curr.getDimension4()).getModel());
	}

	public List<EM> getChartEntityModels() {
		if (chartEntityModels.size() <= 0) {
			initChartModelList();
		}
		return chartEntityModels;
	}

	public void setChartEntityModels(List<EM> chartEntityModels) {
		this.chartEntityModels = chartEntityModels;
	}

	private void configureBarChartModel(BarChartModel chartModel, BarChart barChart) {
		if (barChart.getExtender() != null) {
			chartModel.setExtender(barChart.getExtender());
		}

		chartModel.setStacked(barChart.isStacked());

		Axis xAxis = chartModel.getAxis(AxisType.X);

		if (!StringUtils.isBlank(barChart.getXaxisLabel())) {
			xAxis.setLabel(barChart.getXaxisLabel());
		}

		if (barChart.getXaxisAngle() != null) {
			xAxis.setTickAngle(barChart.getXaxisAngle());
		}
		Axis yAxis = chartModel.getAxis(AxisType.Y);
		if (!StringUtils.isBlank(barChart.getYaxisLabel())) {
			yAxis.setLabel(barChart.getYaxisLabel());
		}
		yAxis.setMin(barChart.getMin());
		yAxis.setMax(barChart.getMax() != null && barChart.getMax() != 0 ? barChart.getMax() : null);
		if (barChart.getYaxisAngle() != null) {
			yAxis.setTickAngle(barChart.getYaxisAngle());
		}

		chartModel.setLegendCols(barChart.getLegendCols());
		chartModel.setLegendRows(barChart.getLegendRows());
		chartModel.setZoom(barChart.isZoom());
		chartModel.setAnimate(barChart.isAnimate());
		chartModel.setShowDatatip(barChart.isShowDataTip());
		if (barChart.getDatatipFormat() != null) {
			chartModel.setDatatipFormat(barChart.getDatatipFormat());
		}
	}

	private void configureLineChartModel(LineChartModel chartModel, LineChart lineChart, ChartSeries chartSeries) {

		LineChartSeries lineChartSeries = new LineChartSeries();
		for (Object o : chartSeries.getData().keySet()) {
			lineChartSeries.set(o.toString(), chartSeries.getData().get(o));
		}
		chartModel.addSeries(lineChartSeries);

		if (lineChart.getExtender() != null) {
			chartModel.setExtender(lineChart.getExtender());
		}

		if (lineChart.getLegendPosition() != null) {
			chartModel.setLegendPosition(lineChart.getLegendPosition().name());
		}
		chartModel.setSeriesColors(lineChart.getSeriesColors());
		chartModel.setShadow(lineChart.isShadow());

		Axis xAxis = chartModel.getAxes().put(AxisType.X, new CategoryAxis("Dates"));
		if (!StringUtils.isBlank(lineChart.getXaxisLabel())) {
			xAxis = chartModel.getAxes().put(AxisType.X, new CategoryAxis(lineChart.getXaxisLabel()));
		}

		/*
		 * if (lineChart.getMinX() < lineChart.getMaxX()) {
		 * xAxis.setMin(lineChart.getMinX()); xAxis.setMax(lineChart.getMaxX());
		 * }
		 */
		if (lineChart.getXaxisAngle() != null) {
			xAxis.setTickAngle(lineChart.getXaxisAngle());
		}

		Axis yAxis = chartModel.getAxis(AxisType.Y);
		if (!StringUtils.isBlank(lineChart.getYaxisLabel())) {
			yAxis.setLabel(lineChart.getYaxisLabel());
		}
		if (lineChart.getMinY() < lineChart.getMaxY()) {
			yAxis.setMin(lineChart.getMinY());
			yAxis.setMax(lineChart.getMaxY());
		}
		if (lineChart.getYaxisAngle() != null) {
			yAxis.setTickAngle(lineChart.getYaxisAngle());
		}

		chartModel.setBreakOnNull(lineChart.isBreakOnNull());

		chartModel.setLegendCols(lineChart.getLegendCols());
		chartModel.setLegendRows(lineChart.getLegendRows());
		chartModel.setStacked(lineChart.isStacked());
		chartModel.setZoom(lineChart.isZoom());
		chartModel.setAnimate(lineChart.isAnimate());
		chartModel.setShowDatatip(lineChart.isShowDataTip());

		if (lineChart.getDatatipFormat() != null) {
			chartModel.setDatatipFormat(lineChart.getDatatipFormat());
		}
	}

	private void configurePieChartModel(PieChartModel chartModel, PieChart pieChart, ChartSeries chartSeries) {

		for (Object o : chartSeries.getData().keySet()) {
			chartModel.set(o.toString(), chartSeries.getData().get(o));
		}
		if (pieChart.getExtender() != null) {
			chartModel.setExtender(pieChart.getExtender());
		}

		chartModel.setFill(pieChart.isFilled());
		if (pieChart.getLegendPosition() != null) {
			chartModel.setLegendPosition(pieChart.getLegendPosition().name());
		}
		chartModel.setSeriesColors(pieChart.getSeriesColors());
		if (pieChart.getDiameter() != null) {
			chartModel.setDiameter(pieChart.getDiameter());
		}

		chartModel.setSliceMargin(pieChart.getSliceMargin());
		chartModel.setShadow(pieChart.isShadow());
		chartModel.setShowDataLabels(pieChart.isShowDataLabels());
		chartModel.setLegendCols(pieChart.getLegendCols());
		chartModel.setLegendRows(pieChart.getLegendRows());
	}

	public List<StatModel> getStats() {
		List<StatModel> result = new ArrayList<StatModel>();
		List<MeasurableQuantity> mqList = mqService.list();

		if (mqList.size() > 0) {
			for (MeasurableQuantity mq : mqList) {
				StatModel sm = new StatModel();
				List<MeasuredValue> mvCurrentPeriod = mvService.getByDateAndPeriod(null, null, null, mq.getMeasurementPeriod(), mq, true);

				if (mvCurrentPeriod.size() > 0) {
					MeasuredValue mv1 = mvCurrentPeriod.get(mvCurrentPeriod.size() - 1);
					sm.setValue(mv1.getValue().doubleValue());
					MeasuredValue mv2 = null;

					if (mvCurrentPeriod.size() > 1) {
						mv2 = mvCurrentPeriod.get(mvCurrentPeriod.size() - 2);
					} else {
						mv2 = new MeasuredValue();
						mv2.setValue(new BigDecimal("0.00"));
					}

					sm.setDescription(mq.getDescription() != null && mq.getDescription().length() > 0 ? mq.getDescription() : mq.getCode());
					sm.computeDifference(mv2.getValue().doubleValue());

					sm.setLastUpdated(mv1.getDate());
					sm.setValue(mv1.getValue().doubleValue());
					result.add(sm);
				}
			}
		}
		return result;
	}

	public StatModel getNewOrderStats() {

		Calendar currentDate = Calendar.getInstance();
		Calendar beforeDate = Calendar.getInstance();

		beforeDate.add(Calendar.DATE, -1);
		beforeDate.set(Calendar.HOUR_OF_DAY, 23);
		beforeDate.set(Calendar.MINUTE, 59);
		beforeDate.set(Calendar.SECOND, 59);
		StatModel result = new StatModel();

		result.setDescription("New Orders");
		result.setLastUpdated(currentDate.getTime().toInstant());

		return result;
	}

	public StatModel getPendingOrderStats() {
		Calendar currentDate = Calendar.getInstance();
		Calendar beforeDate = Calendar.getInstance();
		beforeDate.add(Calendar.DATE, -1);
		StatModel result = new StatModel();

		result.setDescription("Pending Orders");
		result.setLastUpdated(Calendar.getInstance().getTime().toInstant());

		return result;
	}

    public StatModel getChurn() {
		Calendar currentDate = Calendar.getInstance();
		Calendar beforeDate = Calendar.getInstance();

		currentDate.set(Calendar.DATE, 1);
		beforeDate.set(Calendar.DATE, 1);
		beforeDate.add(Calendar.MONTH, -1);
		StatModel result = new StatModel();

		MeasurableQuantity mq = mqService.findByCode("CHURN");

		List<MeasuredValue> currentMVs = mvService.getByDateAndPeriod("CHURN", currentDate.getTime(), null, MeasurementPeriodEnum.MONTHLY, mq);
		MeasuredValue currentMV = (currentMVs != null && currentMVs.size() > 0) ? currentMVs.get(0) : new MeasuredValue();
		Double currentValue = currentMV != null && currentMV.getValue() != null ? currentMV.getValue().doubleValue() : new Double("0.00");

		List<MeasuredValue> beforeMVs = mvService.getByDateAndPeriod("CHURN", beforeDate.getTime(), null, MeasurementPeriodEnum.MONTHLY, mq);
		MeasuredValue beforeMV = (beforeMVs != null && beforeMVs.size() > 0) ? beforeMVs.get(0) : new MeasuredValue();
		Double beforeValue = beforeMV != null && beforeMV.getValue() != null ? beforeMV.getValue().doubleValue() : new Double("0.00");

		log.info("Current MV : " + currentValue + ", Before MV : " + beforeValue);
		result.setValue(currentValue);
		result.setDescription("Churn");
		result.computeDifference(beforeValue);
		result.setLastUpdated(currentDate.getTime().toInstant());
		return result;
	}

    public StatModel getMRR() {
		Calendar currentDate = Calendar.getInstance();
		Calendar beforeDate = Calendar.getInstance();

		currentDate.set(Calendar.DATE, 1);
		beforeDate.set(Calendar.DATE, 1);
		beforeDate.add(Calendar.MONTH, -1);
		StatModel result = new StatModel();

		MeasurableQuantity mq = mqService.findByCode("MRR");

		List<MeasuredValue> currentMVs = mvService.getByDateAndPeriod("MRR", currentDate.getTime(), null, MeasurementPeriodEnum.MONTHLY, mq);
		MeasuredValue currentMV = (currentMVs != null && currentMVs.size() > 0) ? currentMVs.get(0) : new MeasuredValue();
		Double currentValue = currentMV != null && currentMV.getValue() != null ? currentMV.getValue().doubleValue() : new Double("0.00");

		List<MeasuredValue> beforeMVs = mvService.getByDateAndPeriod("MRR", beforeDate.getTime(), null, MeasurementPeriodEnum.MONTHLY, mq);
		MeasuredValue beforeMV = (beforeMVs != null && beforeMVs.size() > 0) ? beforeMVs.get(0) : new MeasuredValue();
		Double beforeValue = beforeMV != null && beforeMV.getValue() != null ? beforeMV.getValue().doubleValue() : new Double("0.00");
		result.setValue(currentValue);
		result.setDescription("MRR");
		result.computeDifference(beforeValue);
		result.setLastUpdated(currentDate.getTime().toInstant());
		return result;
	}

    public List<MeasuredValue> getMeasuredValuesPerYear(String code, String mqCode) {
		return getMeasuredValues(code, mqCode, -13, Calendar.MONTH);
	}

    public List<MeasuredValue> getMeasuredValues(String code, String mqCode, int countFromNow, int period) {
		Calendar currentDate = Calendar.getInstance();
		Calendar beforeDate = Calendar.getInstance();

		currentDate.set(Calendar.DATE, 1);
		beforeDate.set(Calendar.DATE, 1);
		beforeDate.add(period, countFromNow);

		List<MeasuredValue> result = new ArrayList<>();

		MeasurableQuantity mq = mqService.findByCode(mqCode);
		List<MeasuredValue> measuredValues = mvService.getByDateAndPeriod(code, beforeDate.getTime(), currentDate.getTime(),mq == null ? null: mq.getMeasurementPeriod(), mq);
		if(measuredValues != null){
			for(MeasuredValue value : measuredValues){
				result.add(value);
			}
		}

		return result;
	}
}
