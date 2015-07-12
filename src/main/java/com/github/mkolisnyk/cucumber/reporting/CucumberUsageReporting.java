/**
 * .
 */
package com.github.mkolisnyk.cucumber.reporting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.maven.reporting.MavenReportException;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.github.mkolisnyk.cucumber.reporting.types.usage.CucumberStep;
import com.github.mkolisnyk.cucumber.reporting.types.usage.CucumberStepDuration;
import com.github.mkolisnyk.cucumber.reporting.types.usage.CucumberStepSource;
import com.github.mkolisnyk.cucumber.reporting.utils.helpers.MapUtils;

public class CucumberUsageReporting {

    private String       jsonUsageFile;

    private String       outputDirectory;

    public String getDescription(Locale arg0) {
        return "HTML formatted Cucumber keywords usage report";
    }

    public String getName(Locale arg0) {
        return "Cucumber usage report";
    }

    public String getOutputName() {
        return "cucumber-usage-report";
    }

    protected String getOutputDirectory() {
        return this.outputDirectory;
    }

    public String getJsonUsageFile() {
        return jsonUsageFile;
    }

    public void setJsonUsageFile(String jsonUsageFileValue) {
        this.jsonUsageFile = jsonUsageFileValue;
    }

    public void setOutputDirectory(String outputDirectoryValue) {
        this.outputDirectory = outputDirectoryValue;
    }

    public LinkedHashMap<String, Integer> calculateStepsUsageScore(CucumberStepSource[] sources) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();

        for (CucumberStepSource source:sources) {
            int totalSteps = 0;
            for (CucumberStep step:source.getSteps()) {
                totalSteps += step.getDurations().length;
            }
            map.put(source.getSource(), totalSteps);
        }

        map = (LinkedHashMap<String, Integer>) MapUtils.sortByValue(map);

        return map;
    }

    public SortedMap<Integer, Integer> calculateStepsUsageCounts(CucumberStepSource[] sources) {
        SortedMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
        for (CucumberStepSource source:sources) {
            int stepsCount = 0;

            for (CucumberStep step:source.getSteps()) {
                stepsCount += step.getDurations().length;
            }

            if (!map.containsKey(stepsCount)) {
                map.put(stepsCount, 1);
            } else {
                int prevNum = map.get(stepsCount);
                prevNum++;
                map.remove(stepsCount);
                map.put(stepsCount, prevNum);
            }
        }
        return map;
    }

    public double calculateStepsUsageAverage(SortedMap<Integer, Integer> statistics) {
        int totalSteps = 0;
        int totalUniqueSteps = 0;

        for (int i:statistics.keySet()) {
            totalSteps += i * statistics.get(i);
            totalUniqueSteps += statistics.get(i);
        }
        if (totalUniqueSteps == 0) {
            totalUniqueSteps = 1;
        }
        return (double) totalSteps / (double) totalUniqueSteps;
    }

    public int calculateStepsUsageMedian(SortedMap<Integer, Integer> statistics) {
        int totalSteps = 0;
        int usedSteps = 0;
        int median = 0;
        for (int i:statistics.keySet()) {
            totalSteps += statistics.get(i);
        }
        for (int i:statistics.keySet()) {
            usedSteps += statistics.get(i);
            if (usedSteps * 2 >= totalSteps) {
                median = i;
                break;
            }
        }
        return median;
    }

    public int calculateTotalSteps(SortedMap<Integer, Integer> statistics) {
        int totalSteps = 0;

        for (int i:statistics.keySet()) {
            totalSteps += i * statistics.get(i);
        }
        return totalSteps;
    }

    public int calculateUsedSteps(SortedMap<Integer, Integer> statistics) {
        int usedSteps = 0;

        for (int i:statistics.keySet()) {
            usedSteps += statistics.get(i);
        }
        return usedSteps;
    }

    public int calculateStepsUsageMax(SortedMap<Integer, Integer> statistics) {
        int max = 0;
        for (int i:statistics.keySet()) {
            max = Math.max(max, statistics.get(i));
        }
        return max;
    }

    protected String generateUsageOverviewGraphReport(
            CucumberStepSource[] sources) {
        double hscale;
        double vscale;
        final int hsize = 400;
        final int vsize = 400;
        final int hstart = 40;
        final int vstart = 30;
        final int hend = 350;
        final int vend = 300;

        int hstep = 0;
        int vstep = 0;

        int median;
        double average;

        SortedMap<Integer, Integer> map = calculateStepsUsageCounts(sources);
        hscale = (double) (hend - 2 * hstart)
                / ((double) map.lastKey() + 1);
        vscale = (double) (vend - 2 * vstart)
                / ((double) calculateStepsUsageMax(map) + 1);

        final double stepScale = 30.f;

        hstep = (int) (stepScale / hscale) + 1;
        vstep = (int) (stepScale / vscale) + 1;

        median = calculateStepsUsageMedian(map);
        average = calculateStepsUsageAverage(map);

        final int hsizeMargin = 100;
        final int smallMargin = 5;
        final int midMargin = 10;
        final int largeMargin = 20;
        String htmlContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\""
                + (hsize + hsizeMargin)
                + "\" height=\""
                + vsize
                + "\">"
                + "<defs>"
                + "<filter id=\"f1\" x=\"0\" y=\"0\" width=\"200%\" height=\"200%\">"
                + "<feOffset result=\"offOut\" in=\"SourceAlpha\" dx=\"10\" dy=\"10\" />"
                + "<feGaussianBlur result=\"blurOut\" in=\"offOut\" stdDeviation=\"10\" />"
                + "<feBlend in=\"SourceGraphic\" in2=\"blurOut\" mode=\"normal\" />"
                + "</filter>"
                + "<radialGradient id=\"grad1\" cx=\"0%\" cy=\"100%\" r=\"150%\" fx=\"0%\" fy=\"100%\">"
                + "<stop offset=\"0%\" style=\"stop-color:white;stop-opacity:0.1\" />"
                + "<stop offset=\"100%\" style=\"stop-color:gold;stop-opacity:0.7\" />"
                + "</radialGradient>"
                + "<linearGradient id=\"grad2\" cx=\"0%\" cy=\"100%\" r=\"150%\" fx=\"0%\" fy=\"100%\">"
                + "<stop offset=\"0%\" style=\"stop-color:red;stop-opacity:0.7\" />"
                + "<stop offset=\"50%\" style=\"stop-color:yellow;stop-opacity:0.7\" />"
                + "<stop offset=\"100%\" style=\"stop-color:green;stop-opacity:0.7\" />"
                + "</linearGradient>"
                + "</defs>"
                + "<rect width=\"90%\" height=\"90%\" stroke=\"black\" "
                + "stroke-width=\"1\" fill=\"url(#grad1)\" filter=\"url(#f1)\" />"
                + "<line x1=\"" + (hstart) + "\" y1=\"" + (vstart) + "\" x2=\"" + (hstart) + "\" y2=\"" + (vend)
                + "\" style=\"stroke:black;stroke-width:1\" />"
                + "<line x1=\"" + (hstart) + "\" y1=\"" + (vend) + "\" x2=\"" + (hend) + "\" y2=\"" + (vend)
                + "\" style=\"stroke:black;stroke-width:1\" />"
                + "<polygon points=\"" + (hstart - smallMargin) + "," + (vstart + largeMargin) + " " + (hstart) + ","
                + (vstart) + " " + (hstart + smallMargin) + "," + (vstart + largeMargin)
                + "\" style=\"fill:black;stroke:black;stroke-width:1\"/>"
                + "<polygon points=\"" + (hend) + "," + (vend) + " " + (hend - largeMargin) + ","
                + (vend + smallMargin) + " " + (hend - largeMargin) + "," + (vend - smallMargin)
                + "\" style=\"fill:black;stroke:black;stroke-width:1\"/>"
                + "<polygon points=\"" + hstart + "," + vend;
        for (int i = 0; i <= map.lastKey() + 1; i++) {
            int value = 0;
            if (map.containsKey(i)) {
                value = map.get(i);
            }
            htmlContent += " " + (hstart + (int) (i * hscale)) + ","
                    + (vend - (int) (value * vscale));
        }
        htmlContent += "\" style=\"stroke:black;stroke-width:1\"  fill=\"url(#grad2)\" />";

        for (int i = 0; i <= map.lastKey(); i += hstep) {
            htmlContent += "<line x1=\""
                    + (hstart + (int) (i * hscale)) + "\" y1=\""
                    + (vend) + "\" x2=\""
                    + (hstart + (int) (i * hscale)) + "\" y2=\""
                    + (vend + smallMargin)
                    + "\" style=\"stroke:black;stroke-width:1\" />"
                    + "<text x=\"" + (hstart + (int) (i * hscale))
                    + "\" y=\"" + (vend + midMargin)
                    + "\" font-size = \"8\">" + i + "</text>";
        }

        for (int i = 0; i <= calculateStepsUsageMax(map); i += vstep) {
            htmlContent += "<line x1=\"" + (hstart) + "\" y1=\""
                    + (vend - (int) (i * vscale)) + "\" x2=\""
                    + (hstart - smallMargin) + "\" y2=\""
                    + (vend - (int) (i * vscale))
                    + "\" style=\"stroke:black;stroke-width:1\" />"
                    + "<text x=\"" + (hstart - smallMargin) + "\" y=\""
                    + (vend - (int) (i * vscale))
                    + "\" transform=\"rotate(-90 " + (hstart - smallMargin)
                    + "," + (vend - (int) (i * vscale))
                    + ")\" font-size = \"8\">" + i + "</text>";
        }

        final float usage30 = 30.f;
        final float usage70 = 70.f;
        final float usage100 = 100.f;
        float usage = usage100 * (1.f - (float) calculateUsedSteps(map)
                / (float) calculateTotalSteps(map));
        String statusColor = "silver";

        if (usage <= usage30) {
            statusColor = "red";
        } else if (usage >= usage70) {
            statusColor = "green";
        } else {
            statusColor = "#BBBB00";
        }

        htmlContent += "<line stroke-dasharray=\"10,10\" x1=\"" + (hstart + median * hscale) + "\" y1=\"" + (vstart)
                + "\" x2=\"" + (hstart + median * hscale) + "\" y2=\"" + vend
                + "\" style=\"stroke:yellow;stroke-width:3\" />"
                + "<line stroke-dasharray=\"10,10\" x1=\"" + (hstart + (int) (average * hscale))
                + "\" y1=\"" + (vstart) + "\" x2=\"" + (hstart + (int) (average * hscale)) + "\" y2=\"" + vend
                + "\" style=\"stroke:red;stroke-width:3\" />"
                + "<rect x=\"60%\" y=\"20%\" width=\"28%\" height=\"20%\" stroke=\"black\""
                + " stroke-width=\"1\" fill=\"white\" filter=\"url(#f1)\" />"
                + "<line x1=\"63%\" y1=\"29%\" x2=\"68%\" y2=\"29%\" stroke-dasharray=\"5,5\""
                + " style=\"stroke:red;stroke-width:3\" /><text x=\"73%\" y=\"30%\""
                + " font-weight = \"bold\" font-size = \"12\">Average</text>"
                + "<line x1=\"63%\" y1=\"34%\" x2=\"68%\" y2=\"34%\" stroke-dasharray=\"5,5\""
                + " style=\"stroke:yellow;stroke-width:3\" /><text x=\"73%\" y=\"35%\""
                + " font-weight = \"bold\" font-size = \"12\">Median</text>"
                + "<text x=\"60%\" y=\"55%\" font-weight = \"bold\" font-size = \"40\" fill=\""
                + statusColor + "\">" + String.format("%.1f", usage)
                + "%</text>"
                + "<text x=\"66%\" y=\"60%\" font-weight = \"bold\" font-size = \"16\" fill=\""
                + statusColor
                + "\">Re-use</text>"
                + "<text x=\"120\" y=\"330\" font-weight = \"bold\" font-size = \"14\" >Step re-use count</text>"
                + "<text x=\"20\" y=\"220\" font-weight = \"bold\" font-size = \"14\""
                + " transform=\"rotate(-90 20,220)\">Steps count</text>"
                + "</svg>";
        return htmlContent;
    }

    private CucumberStepSource getSourceByString(CucumberStepSource[] sources, String text) {
        for (CucumberStepSource source : sources) {
            if (source.getSource().equals(text)) {
                return source;
            }
        }
        return null;
    }
    protected String generateUsageOverviewTableReport(CucumberStepSource[] sources) {
        LinkedHashMap<String, Integer> map = calculateStepsUsageScore(sources);
        String content = "<table><tr><th rowspan=\"2\">Expression</th><th rowspan=\"2\">Occurences</th>"
                + "<th colspan=\"5\">Duration</th></tr>"
                + "<tr>"
                + "<th>Average</th><th>Median</th><th>Minimal</th><th>Maximal</th><th>Total</th></tr>";

        for (String key:map.keySet()) {
            content += "<tr><td width=\"60%\">" + key + "</td><td>" + map.get(key) + "</td>";
            CucumberStepSource source = getSourceByString(sources, key);
            if (source == null) {
                content += "<td>-</td><td>-</td><td>-</td><td>-</td><td>-</td>";
            } else {
                List<Double> durations = source.getDurations();
                if (durations.size() <= 0) {
                    return "";
                }
                Collections.sort(durations);
                Double median = durations.get(durations.size() / 2);
                Double total = 0.;
                for (Double duration : durations) {
                    total += duration;
                }
                Double average = total / durations.size();
                Double min = Collections.min(durations);
                Double max = Collections.max(durations);
                content += String.format(
                        "<td>%.2fs</td><td>%.2fs</td><td>%.2fs</td><td>%.2fs</td><td>%.2fs</td>",
                        average, median, min, max, total);
            }
            content += "</tr>";
        }
        content += "</table>";
        return content;
    }

    private String generateSourceDurationOverview(CucumberStepSource source) {
        List<Double> durations = source.getDurations();
        if (durations.size() <= 0) {
            return "";
        }
        Collections.sort(durations);
        Double median = durations.get(durations.size() / 2);
        Double total = 0.;
        for (Double duration : durations) {
            total += duration;
        }
        Double average = total / durations.size();
        Double min = Collections.min(durations);
        Double max = Collections.max(durations);
        return String.format("<p><table>"
                + "<tr><th colspan=\"2\">Duration Characteristic</th></tr>"
                + "<tr><th>Average</th><td>%.2fs</td></tr>"
                + "<tr><th>Median</th><td>%.2fs</td></tr>"
                + "<tr><th>Minimum</th><td>%.2fs</td></tr>"
                + "<tr><th>Maximum</th><td>%.2fs</td></tr>"
                + "<tr><th>Total</th><td>%.2fs</td></tr>"
                + "</table></p>", average, median, min, max, total);
    }

    protected String generateUsageDetailedReport(CucumberStepSource[] sources) {
        String content = "";
        for (CucumberStepSource source:sources) {

            content += "<h3>" + source.getSource() + "</h3>"
                    + "<table><tr><th>Step Name</th><th>Duration</th><th>Location</th></tr>";

            for (CucumberStep step:source.getSteps()) {
                content += "<tr><td>" + step.getName() + "</td><td> - </td><td> - </td></tr>";
                for (CucumberStepDuration duration:step.getDurations()) {
                    content += "<tr><td></td><td>" + duration.getDuration()
                            + "</td><td>" + duration.getLocation() + "</td></tr>";
                }
            }
            content += "</table>";
            content += generateSourceDurationOverview(source);
        }
        return content;
    }

    @SuppressWarnings("unchecked")
    public CucumberStepSource[] getStepSources(String filePath) throws Exception {
        FileInputStream fis = null;
        JsonReader jr = null;
        File file = new File(filePath);

        if (!(file.exists() && file.isFile())) {
            throw new FileNotFoundException();
        }

        fis = new FileInputStream(file);
        jr = new JsonReader(fis, true);
        JsonObject<String, Object> source = (JsonObject<String, Object>) jr.readObject();
        Object[] objs = (Object[]) source.get("@items");

        CucumberStepSource[] sources = new CucumberStepSource[objs.length];
        for (int i = 0; i < objs.length; i++) {
            sources[i] = new CucumberStepSource((JsonObject<String, Object>) objs[i]);
        }
        jr.close();
        fis.close();
        return sources;
    }

    private String generateStyle() {
        return "h1 {background-color:#9999CC}" + System.lineSeparator()
            + "h2 {background-color:#BBBBCC}" + System.lineSeparator()
            + "h3 {background-color:#DDDDFF}" + System.lineSeparator()
            + "th {border:1px solid black;background-color:#CCCCDD;}" + System.lineSeparator()
            + "td{border:1px solid black;}" + System.lineSeparator()
            + "table{border:1px solid black;border-collapse: collapse;}" + System.lineSeparator()
            + "tr:nth-child(even) {background: #CCC}" + System.lineSeparator()
            + "tr:nth-child(odd) {background: #FFF}";
    }

    public void executeReport() throws MavenReportException {
        try {

            CucumberStepSource[] sources = getStepSources(jsonUsageFile);

            String output = "<html><head><style type=\"text/css\">"
                    + generateStyle()
                    + "</style>"
                    + "<title>Cucumber Steps Usage Report</title></head>"
                    + "<body><h1>Cucumber Usage Statistics</h1>"
                    + "<h2>Overview Graph</h2><p>"
                    + generateUsageOverviewGraphReport(sources)
                    + "</p>"
                    + "<h2>Overview Table</h2><p>"
                    + generateUsageOverviewTableReport(sources)
                    + "</p>"
                    + "<h1>Cucumber Usage Detailed Information</h1><p>"
                    + generateUsageDetailedReport(sources)
                    + "</p></body></html>";
            File report = new File(this.getOutputDirectory() + File.separator + this.getOutputName() + ".html");
            FileUtils.writeStringToFile(report, output);
        } catch (Exception e) {
            throw new MavenReportException(
                    "Error occured while generating Cucumber usage report", e);
        }
    }
}
