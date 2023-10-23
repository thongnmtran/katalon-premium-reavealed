import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

import org.apache.commons.lang3.StringUtils

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.exception.KatalonRuntimeException
import com.kms.katalon.core.logging.ErrorCollector
import com.kms.katalon.core.logging.KeywordLogger
import com.kms.katalon.core.main.ScriptEngine
import com.kms.katalon.core.util.ConsoleCommandBuilder
import com.kms.katalon.core.util.KeywordUtil

import internal.GlobalVariable

public class SecuredProfile {
	private static ScriptEngine engine;
	static {
		GroovyClassLoader classLoader = new GroovyClassLoader(SecuredProfile.class.getClassLoader());
		engine = ScriptEngine.getDefault(classLoader);
	}

	@Keyword
	def defineSecuredProfiles(String ...profilePaths) {
		if (profilePaths.length == 0) {
			profilePaths = []; // All other profiles in the Profiles folder
		}

		List<String> profiles = Arrays.asList(profilePaths).stream().filter({ profileI ->
			return StringUtils.isNotBlank(profileI);
		}).collect(Collectors.toList());
		if (profiles.isEmpty()) {
			return;
		}

		for (String profileI : profiles) {
			String validProfilePath = loadProfile(profileI);
			if (isExternalFile(profileI)) {
				continue;
			}
			ignoreFile(validProfilePath);
		}
	}

	@Keyword
	def void ignoreFile(String filePath) {
		if (isExternalFile(filePath)) {
			return;
		}

		File gitIgnoreFile = fileFromRelativePath(".gitignore");
		Path gitIgnorePath = gitIgnoreFile.toPath();
		String gitIgnoreContent = Files.readString(gitIgnorePath);
		if (StringUtils.isBlank(gitIgnoreContent)) {
			gitIgnoreContent = "";
		}

		String relativePath = getRelativePath(filePath);
		String ignoreLine = "/" + relativePath;
		if (StringUtils.contains(gitIgnoreContent, ignoreLine)) {
			return;
		}

		removeFileFromGit(relativePath);

		if (!gitIgnoreContent.endsWith("\n\n")) {
			if (gitIgnoreContent.endsWith("\n")) {
				gitIgnoreContent += "\n";
			} else {
				gitIgnoreContent += "\n\n";
			}
		}
		gitIgnoreContent += ignoreLine + "\n";

		Files.writeString(gitIgnorePath, gitIgnoreContent);

		addFileToGit(gitIgnorePath.toString());
	}

	@Keyword
	def String loadProfile(String profilePath) {
		String validProfilePath = findProfile(profilePath);
		def profile = parseProfile(validProfilePath);
		for (Map.Entry<String,String> entry : profile.entrySet()) {
			GlobalVariable.putAt(entry.getKey(), entry.getValue());
		}
		return validProfilePath;
	}

	protected String findProfile(String profilePath) {
		List<String> variants = new ArrayList<String>(Arrays.asList(profilePath, profilePath + ".glbl"));
		if (!new File(profilePath).isAbsolute()) {
			variants.add(absolutePathFromRelativePath("./Profiles/" + profilePath));
			variants.add(absolutePathFromRelativePath("./Profiles/" + profilePath + ".glbl"));
		}

		String validProfilePath = "";
		for (String variantI : variants) {
			if (new File(variantI).exists()) {
				validProfilePath = variantI;
				break;
			}
		}
		if (StringUtils.isBlank(validProfilePath)) {
			throw new Exception("Profile does not exist: \"" + profilePath + "\"");
		}
		return validProfilePath;
	}

	protected Map<String, Object> parseProfile(String profilePath) {
		String profileName = new File(profilePath).getName();
		try {
			Map<String, Object> selectedVariables = new HashMap<>();
			Node rootNode = new XmlParser().parse(new File(profilePath));
			NodeList variableNodes = (NodeList) rootNode.get("GlobalVariableEntity");
			for (int index = 0; index < variableNodes.size(); index++) {
				Node globalVariableNode = (Node) variableNodes.get(index);
				String variableName = ((Node) ((NodeList) globalVariableNode.get("name")).get(0)).text();
				String defaultValue = ((Node) ((NodeList) globalVariableNode.get("initValue")).get(0)).text();
				try {
					selectedVariables.put(variableName, SecuredProfile.engine.runScriptWithoutLogging(defaultValue, new Binding()));
				} catch (Exception e) {
					KatalonRuntimeException runtimeException = new KatalonRuntimeException(String.format(
							"Could not evaluate default value for variable: %s of profile: \"%s\". Details: %s",
							variableName, profileName, e.getMessage()), e);
					ErrorCollector.getCollector().addError(runtimeException);
				}
			}
			return selectedVariables;
		} catch (Exception ex) {
			KatalonRuntimeException runtimeException = new KatalonRuntimeException(
					String.format("Could not evaluate variable of profile: %s. Details: %s", profileName, ex), ex);
			ErrorCollector.getCollector().addError(runtimeException);
			return Collections.emptyMap();
		}
	}

	protected void addFileToGit(String filePath) {
		if (isGitIntegrated()) {
			try {
				ConsoleCommandBuilder.create("git add \"" + filePath + "\"").execSync();
			} catch (Exception error) {
				KeywordLogger.getInstance(getClass()).logWarning("Unable to add the target file from your git repo. Please do it manually (\"${filePath}\")");
			}
		}
	}

	protected void removeFileFromGit(String filePath) {
		if (isGitIntegrated()) {
			try {
				ConsoleCommandBuilder.create("git rm --cached \"" + filePath + "\"").execSync();
			} catch (Exception error) {
				KeywordLogger.getInstance(getClass()).logWarning("Unable to remove the target file from your git repo. Please do it manually (\"${filePath}\")");
			}
		}
	}

	protected boolean isExternalFile(String filePath) {
		File root = new File(getProjectFolder());
		String relativePath = root.relativePath(new File(filePath));
		return relativePath.startsWith("..");
	}

	protected boolean isGitIntegrated() {
		File root = fileFromRelativePath(".");
		while (root != null && root.exists()) {
			File gitFolder = new File(root, ".git");
			if (gitFolder.exists()) {
				return true;
			}
			root = root.getParentFile();
		}
		return false;
	}

	protected String getRelativePath(String filePath) {
		return fileFromRelativePath(".").relativePath(new File(filePath));
	}

	protected String absolutePathFromRelativePath(String relativePath) {
		if (new File(relativePath).isAbsolute()) {
			return relativePath;
		}
		return fileFromRelativePath(relativePath).getCanonicalPath();
	}

	protected File fileFromRelativePath(String relativePath) {
		return new File(getProjectFolder(), relativePath).getCanonicalFile();
	}

	protected String getProjectFolder() {
		return RunConfiguration.getProjectDir();
	}
}
