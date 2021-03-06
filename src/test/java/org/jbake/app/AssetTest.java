package org.jbake.app;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.io.FileUtils;
import org.jbake.app.ConfigUtil.Keys;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AssetTest {

    private CompositeConfiguration config;

	@Before
    public void setup() throws Exception {
		config = ConfigUtil.load(new File(this.getClass().getResource("/fixture").getFile()));
		Assert.assertEquals(".html", config.getString(Keys.OUTPUT_EXTENSION));
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void copy() throws Exception {
		URL assetsUrl = this.getClass().getResource("/fixture/assets");
		File assets = new File(assetsUrl.getFile());
		Asset asset = new Asset(assets.getParentFile(), folder.getRoot(), config);
		asset.copy(assets);

		File cssFile = new File(folder.getRoot().getPath() + File.separatorChar + "css" + File.separatorChar + "bootstrap.min.css");
		Assert.assertTrue("File " + cssFile.getAbsolutePath() + " does not exist", cssFile.exists());
		File imgFile = new File(folder.getRoot().getPath() + File.separatorChar + "img" + File.separatorChar + "glyphicons-halflings.png");
		Assert.assertTrue("File " + imgFile.getAbsolutePath() + " does not exist", imgFile.exists());
		File jsFile = new File(folder.getRoot().getPath() + File.separatorChar + "js" + File.separatorChar + "bootstrap.min.js");
		Assert.assertTrue("File " + jsFile.getAbsolutePath() + " does not exist", jsFile.exists());

		Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
	}

	@Test
	public void copyCustomFolder() throws Exception {
		config.setProperty(ConfigUtil.Keys.ASSET_FOLDER, "media");
		URL assetsUrl = this.getClass().getResource("/fixture/media");
		File assets = new File(assetsUrl.getFile());
		Asset asset = new Asset(assets.getParentFile(), folder.getRoot(), config);
		asset.copy(assets);

		File favFile = new File(folder.getRoot().getPath() + File.separatorChar + "favicon.ico");
		Assert.assertTrue("File " + favFile.getAbsolutePath() + " does not exist", favFile.exists());

		Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
	}

	@Test
	public void copyIgnore() throws Exception {
		config.setProperty(Keys.ASSET_FOLDER, "ignorables");
		config.setProperty(Keys.ASSET_IGNORE_HIDDEN, "true");
		URL assetsUrl = this.getClass().getResource("/fixture/ignorables");
		File assets = new File(assetsUrl.getFile());
		hideAssets(assets);
		Asset asset = new Asset(assets.getParentFile(), folder.getRoot(), config);
		asset.copy(assets);

		File testFile = new File(folder.getRoot(), "test.txt");
		Assert.assertTrue("File " + testFile.getAbsolutePath() + " does not exist", testFile.exists());
		File testIgnoreFile = new File(folder.getRoot(), ".test.txt");
		Assert.assertFalse("File " + testIgnoreFile.getAbsolutePath() + " does exist", testIgnoreFile.exists());

		Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
	}

	/**
	 * Hides the assets on windows that start with a dot (e.g. .test.txt but not test.txt) so File.isHidden() returns true for those files.
	 */
	private void hideAssets(File assets) throws IOException, InterruptedException {
		if (isWindows()) {
			final File[] hiddenFiles = assets.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith(".");
				}
			});
			for (File file : hiddenFiles) {
				final Process process = Runtime.getRuntime().exec(new String[] {"attrib" , "+h", file.getAbsolutePath()});
				process.waitFor();
			}
		}
	}

	/**
	 * Primary intention is to extend test cases to increase coverage.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWriteProtected() throws Exception {
		URL assetsUrl = this.getClass().getResource("/fixture/assets");
		File assets = new File(assetsUrl.getFile());
		final File cssFile = new File(folder.newFolder("css"), "bootstrap.min.css");
		FileUtils.touch(cssFile);
		cssFile.setReadOnly();
		Asset asset = new Asset(assets.getParentFile(), folder.getRoot(), config);
		asset.copy(assets);

		Assert.assertFalse("At least one error during copy expected", asset.getErrors().isEmpty());
	}

	/**
	 * Primary intention is to extend test cases to increase coverage.
	 *
	 * @throws Exception
	 */
	@Test
	public void testUnlistable() throws Exception {
		config.setProperty(Keys.ASSET_FOLDER, "non-existent");
		URL assetsUrl = this.getClass().getResource("/fixture");
		File assets = new File(assetsUrl.getFile() + File.separatorChar + "non-existent");
		Asset asset = new Asset(assets.getParentFile(), folder.getRoot(), config);
		asset.copy(assets);
	}

	private boolean isWindows() {
		final String os = System.getProperty("os.name");
		return os != null && os.toLowerCase(Locale.ENGLISH).contains("win");
	}
}
