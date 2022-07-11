package org.meveo.api.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.ejb.Stateless;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.admin.FileDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.StringUtils;

/**
 * @author Edward P. Legaspi
 * @author Wassim Drira
 * @lastModifiedVersion 5.0
 * 
 */
@Stateless
public class FilesApi extends BaseApi {

    public String getProviderRootDir() {
        return paramBeanFactory.getChrootDir();
    }

    public List<FileDto> listFiles(String dir) throws BusinessApiException {
        if (!StringUtils.isBlank(dir)) {
            dir = getProviderRootDir() + File.separator + dir;
        } else {
            dir = getProviderRootDir();
        }

        File folder = new File(dir);

        if (folder.isFile()) {
            throw new BusinessApiException("Path " + dir + " is a file.");
        }

        List<FileDto> result = new ArrayList<FileDto>();

        if (folder.listFiles() != null && folder.listFiles().length > 0) {
            List<File> files = Arrays.asList(folder.listFiles());
            if (files != null) {
                for (File file : files) {
                    result.add(new FileDto(file));
                }
            }
        }

        return result;
    }

    public void createDir(String dir) throws BusinessApiException {
        File file = new File(getProviderRootDir() + File.separator + dir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public void zipFile(String filePath) throws BusinessApiException {
        File file = new File(getProviderRootDir() + File.separator + filePath);
        if (!file.exists()) {
            throw new BusinessApiException("File does not exists: " + file.getPath());
        }

        try {
            FileUtils.archiveFile(file);
        } catch (IOException e) {
            throw new BusinessApiException("Error zipping file: " + file.getName() + ". " + e.getMessage());
        }
    }

    public void zipDir(String dir) throws BusinessApiException {
        File file = new File(getProviderRootDir() + File.separator + dir);
        if (!file.exists()) {
            throw new BusinessApiException("Directory does not exists: " + file.getPath());
        }

        try {
            FileOutputStream fos = new FileOutputStream(new File(FilenameUtils.removeExtension(file.getParent() + File.separator + file.getName()) + ".zip"));
            ZipOutputStream zos = new ZipOutputStream(fos);
            FileUtils.addDirToArchive(getProviderRootDir(), file.getPath(), zos);
            fos.flush();
            zos.close();
            fos.close();
        } catch (IOException e) {
            throw new BusinessApiException("Error zipping directory: " + file.getName() + ". " + e.getMessage());
        }
    }

    public void uploadFile(byte[] data, String filename) throws BusinessApiException {
        File file = new File(getProviderRootDir() + File.separator + filename);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fop = new FileOutputStream(file);

            fop.write(data);
            fop.flush();
            fop.close();

            if (FilenameUtils.getExtension(file.getName()).equals("zip")) {
                // unzip
                // get parent dir
                String parentDir = file.getParent();
                FileUtils.unzipFile(parentDir, new FileInputStream(file));
            }

        } catch (Exception e) {
            throw new BusinessApiException("Error uploading file: " + filename + ". " + e.getMessage());
        }
    }

    public void suppressFile(String filePath) throws BusinessApiException {
        String filename = getProviderRootDir() + File.separator + filePath;
        File file = new File(filename);

        if (file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                throw new BusinessApiException("Error suppressing file: " + filename + ". " + e.getMessage());
            }
        } else {
            throw new BusinessApiException("File does not exists: " + filename);
        }
    }

    public void suppressDir(String dir) throws BusinessApiException {
        String filename = getProviderRootDir() + File.separator + dir;
        File file = new File(filename);

        if (file.exists()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(file);
            } catch (Exception e) {
                throw new BusinessApiException("Error suppressing file: " + filename + ". " + e.getMessage());
            }
        } else {
            throw new BusinessApiException("Directory does not exists: " + filename);
        }
    }

    public void downloadFile(String filePath, HttpServletResponse response) throws BusinessApiException {
        File file = new File(getProviderRootDir() + File.separator + filePath);
        if (!file.exists()) {
            throw new BusinessApiException("File does not exists: " + file.getPath());
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            response.setContentType(Files.probeContentType(file.toPath()));
            response.setContentLength((int) file.length());
            response.addHeader("Content-disposition", "attachment;filename=\"" + file.getName() + "\"");
            IOUtils.copy(fis, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new BusinessApiException("Error zipping file: " + file.getName() + ". " + e.getMessage());
        }
    }

    public boolean checkFile(String filePath) {
        String filename = getProviderRootDir() + File.separator + filePath;
        File file = new File(filename);

        if (!file.exists()) {
           return false;
        }
        return true;
    }

    public void renameFolder(String oldFolder, String newFolder) throws BusinessApiException {
        if (oldFolder != null && newFolder != null) {
            String folderPath = getProviderRootDir() + File.separator + oldFolder;
            File currentFolder = new File(folderPath);
            if (!currentFolder.exists() && !currentFolder.isDirectory()) {
                throw new BusinessApiException("Folder " + currentFolder.getPath() + " does not exists");
            }
            String[] data = oldFolder.split("\\\\");
            StringBuilder path = new StringBuilder(File.separator);
            if (data.length > 2) {
                for (int i = 1; i < data.length - 1; i++) {
                    path.append(data[i]).append(File.separator);
                }
                newFolder = path.toString() + newFolder;
            }
            String newFolderPath = getProviderRootDir() + File.separator + newFolder;
            File newDir = new File(newFolderPath);
            if (!newDir.exists()) {
                if (!currentFolder.renameTo(newDir)) {
                    throw new BusinessApiException("Cannot rename "  + currentFolder.getPath() + " to " + newDir.getPath());
                }
            } else {
                throw new BusinessApiException("Folder "  + newDir.getPath() + " already exists in file explorer");
            }
        }
    }

}
