package integration.myschedule.quartz.extra;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import myschedule.quartz.extra.ProcessUtils;
import myschedule.quartz.extra.SchedulerMain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.SchedulerPlugin;

public class SchedulerMainIT {
	public static File PLUGIN_RESULT_FILE = createTempFile("SchedulerMainIT-ResultSchedulerPlugin.tmp");
	
	public static File createTempFile(String filename) {
		return new File(System.getProperty("java.io.tmpdir") + "/" + filename);
	}

	public static void resetResult() {
		// Reset file content
		try {
			FileUtils.writeStringToFile(PLUGIN_RESULT_FILE, "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void writeResult(String text) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(PLUGIN_RESULT_FILE, true);
			IOUtils.write(text, writer);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	@Test
	public void testMainWithTimeout() throws Exception {		
		try {
			// Run SchedulerMain with timeout settings so it should exit automatically.
			String config = "integration/myschedule/quartz/extra/SchedulerMainIT-quartz.properties";
			String[] javaCmdArgs = { SchedulerMain.class.getName(), config };
			String[] javaOpts = { "-DSchedulerMain.Timeout=700" };
			ProcessUtils.runJavaWithOpts(3000, javaOpts, javaCmdArgs);
			
			List<String> result = FileUtils.readLines(PLUGIN_RESULT_FILE);
			assertThat(result.size(), is(4));
			assertThat(result.get(0), is("name: MyResultSchedulerPluginTest"));
			assertThat(result.get(1), containsString("initialize:"));
			assertThat(result.get(2), containsString("start:"));
			assertThat(result.get(3), containsString("shutdown:"));
		} finally {
			PLUGIN_RESULT_FILE.delete();
		}
	}
	
	@Test
	public void testMainAsServer() throws Exception {		
		try {
			try {
				// Default SchedulerMain will run as server, so this should cause test to timeout.
				String config = "integration/myschedule/quartz/extra/SchedulerMainIT-quartz.properties";
				ProcessUtils.runJava(700, SchedulerMain.class.getName(), config);
				fail("We should have timed-out, but didn't.");
			} catch (ProcessUtils.TimeoutException e) {
				// expected.
			}
			List<String> result = FileUtils.readLines(PLUGIN_RESULT_FILE);
			assertThat(result.size(), is(3));
			assertThat(result.get(0), is("name: MyResultSchedulerPluginTest"));
			assertThat(result.get(1), containsString("initialize:"));
			assertThat(result.get(2), containsString("start:"));
			
			// Note we don't have shutdown due to timeout!
		} finally {
			PLUGIN_RESULT_FILE.delete();
		}
	}
	
	public static class ResultSchedulerPlugin implements SchedulerPlugin {
		public ResultSchedulerPlugin() {
			resetResult();
		}
		
		@Override
		public void initialize(String name, Scheduler scheduler) throws SchedulerException {
			writeResult("name: " + name + "\n");
			writeResult("initialize: " + new Date() + "\n");
		}

		@Override
		public void start() {
			writeResult("start: " + new Date() + "\n");
		}

		@Override
		public void shutdown() {
			writeResult("shutdown: " + new Date() + "\n");
		}
	}
}