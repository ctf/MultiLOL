package ca.mcgill.sus.multilol;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

public class Main {
	public static void main(String[] args) throws IOException {
		String appData = System.getenv("LocalAppData");
		File lolDir = new File(appData+"\\LOL");
		lolDir.mkdir();
		File radsDir = new File(appData+"\\LOL\\RADS");
		radsDir.mkdir();
		String lolInstallDir = System.getenv("SystemDrive")+"\\Riot Games\\League of Legends";
		Path launcherDir = new File(lolInstallDir).toPath();
		boolean success = false;
		for (int i = 0; i < 3 && success == false; i++) {
			try {
				copyFiles(launcherDir, lolDir.toPath(), new String[]{"lol.launcher.exe"});
				copyFiles(new File(launcherDir+"\\RADS").toPath(), radsDir.toPath(), new String[]{"solutions","system"});
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
				Runtime.getRuntime().exec("taskkill /f /im lollauncher.exe", null, new File(System.getenv("SystemDrive")+"\\"));
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		if (!success) {
			JOptionPane.showMessageDialog(null,"The launcher is already running but not responding, please terminate it.","League of Legends",JOptionPane.ERROR_MESSAGE);
			return;
		}
		File projectsDir = new File(appData+"\\LOL\\RADS\\projects");
		projectsDir.mkdir();
		massLink(new File(launcherDir+"\\RADS\\projects").toPath(), projectsDir.toPath(), new String[]{"lol_air_client_config_na","lol_game_client","lol_game_client_en_us","lol_launcher"});
		String[] releases = new File(launcherDir+"\\RADS\\projects\\lol_air_client\\releases").list();
		Arrays.sort(releases);
		File lolAirClientSourceDir = new File(launcherDir+"\\RADS\\projects\\lol_air_client\\releases\\"+releases[0]);
		File lolAirClientDir = new File(appData+"\\LOL\\RADS\\projects\\lol_air_client\\releases\\"+releases[0]);
		lolAirClientDir.mkdirs();
		copyFiles(lolAirClientSourceDir.toPath(), lolAirClientDir.toPath(), new String[]{"releasemanifest","S_OK"});
		File deploySourceDir = new File(launcherDir+"\\RADS\\projects\\lol_air_client\\releases\\"+releases[0]+"\\deploy");
		File deployDir = new File(appData+"\\LOL\\RADS\\projects\\lol_air_client\\releases\\"+releases[0]+"\\deploy");
		deployDir.mkdir();
		copyFiles(deploySourceDir.toPath(), deployDir.toPath(), new String[]{"locale.properties","lol.properties","LolClient.exe","LolClient.swf","mimetype"});
		massLink(deploySourceDir.toPath(), deployDir.toPath(), new String[]{"Adobe Air","assets","css","lib","logs","META-INF","mod"});
		File preferencesSourceDir = new File(System.getenv("AppData")+"\\LOL\\preferences");
		Path roamingLolDir = new File(System.getenv("AppData")+"\\LOL").toPath();
		if (preferencesSourceDir.exists()) {
			try {
				copyFiles(roamingLolDir,deployDir.toPath(),new String[]{"preferences"});
				copyFiles(roamingLolDir,lolDir.toPath(),new String[]{"Config"});
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			preferencesSourceDir.mkdirs();
			File preferencesDir = new File(appData+"\\LOL\\RADS\\projects\\lol_air_client\\releases\\"+releases[0]+"\\deploy\\preferences");
			preferencesDir.mkdir();
		}
		Runtime.getRuntime().exec(lolDir+"\\lol.launcher.exe", null, lolDir);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while(isProcessRunning("LoLLauncher.exe")) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		copyFiles(deployDir.toPath(),roamingLolDir,new String[]{"preferences"});
		copyFiles(lolDir.toPath(),roamingLolDir,new String[]{"Config"});
	}
	public static void makeJunctionPoint(Path link, Path target) {
		try {
			/*Process p =*/ Runtime.getRuntime().exec("cmd /c mklink /j \""+link+"\" \""+target+"\"", null, new File(System.getenv("SystemDrive")+"\\"));
//			String err = dumpStream(p.getErrorStream());
//			String out = dumpStream(p.getInputStream());
		} catch (Exception e) {
		}
	}
	public static String dumpStream(InputStream is) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		r.close();
		return sb.toString().trim();
	}
	public static void copyFiles(Path sourceDir, Path targetDir, String[] filenames) throws IOException {
		for (String filename : filenames) {
			final Path sourceFile = new File(sourceDir+"\\"+filename).toPath();
			final Path targetFile = new File(targetDir+"\\"+filename).toPath();
			if (sourceFile.toFile().isDirectory()) {
				Files.walkFileTree(sourceFile, new SimpleFileVisitor<Path>() {
				    private Path fromPath = sourceFile;
				    private Path toPath = targetFile;
				    private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;
				    @Override
				    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				        Path targetPath = toPath.resolve(fromPath.relativize(dir));
				        if(!Files.exists(targetPath)){
				            Files.createDirectory(targetPath);
				        }
				        return FileVisitResult.CONTINUE;
				    }

				    @Override
				    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				        Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
				        return FileVisitResult.CONTINUE;
				    }
				});
			} else {
				Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
			
		}
	}
	public static void massLink(Path sourceDir, Path targetDir, String[] dirNames) throws IOException {
		for (String dirName : dirNames) {
			final Path sourceFile = new File(sourceDir+"\\"+dirName).toPath();
			final Path targetFile = new File(targetDir+"\\"+dirName).toPath();
			makeJunctionPoint(targetFile, sourceFile);
		}
	}
	public static String[] getRunningProcesses() {
	    try {
	        String line;
	        Process p = Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\"+"tasklist.exe");
	        BufferedReader input =
	                new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String divider = "========================= ======== ================ =========== ============";
	        boolean write = false;
	        ArrayList<String> processes = new ArrayList<String>();
	        while ((line = input.readLine()) != null) {
	        	if (write) {
	        		processes.add(line);
	        	}
	            if (line.equals(divider)) write = true;
	        }
	        input.close();
	        return processes.toArray(new String[]{});
	    } catch (Exception err) {
	        err.printStackTrace();
	    }
		return new String[]{};
	}
	public static boolean isProcessRunning(String processName) {
		String[] processes = getRunningProcesses();
		for (String process : processes) {
			if (process.toLowerCase().contains(processName.toLowerCase())) return true;
		}
		return false;
	}
}
