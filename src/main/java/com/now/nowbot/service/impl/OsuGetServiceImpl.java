package com.now.nowbot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.entity.BeatMapFileLite;
import com.now.nowbot.mapper.BeatMapFileRepository;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.util.AsyncMethodExecutor;
import com.now.nowbot.util.JacksonUtil;
import com.now.nowbot.util.OsuMapDownloadUtil;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;

// qnmd, 瞎 warning
@SuppressWarnings("all")
// ??

public class OsuGetServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(OsuGetServiceImpl.class);


    RestTemplate template;

    FileConfig fileConfig;

    OsuMapDownloadUtil osuMapDownloadUtil;

    BeatMapFileRepository beatMapFileRepository;



    public boolean downloadAllFiles(long sid) throws IOException {
        try {
            return AsyncMethodExecutor.execute(() -> doDownload(sid), sid, false);
        } catch (IOException ioException) {
            throw ioException;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean doDownload(long sid) throws IOException {
        Path tmp = Path.of(fileConfig.getOsuFilePath(), "tmp-" + sid);
        if (Files.exists(tmp)) {
            deleteDirTree(tmp);
        }
        HashMap<String, Path> fileMap = new HashMap<>();
        var account = osuMapDownloadUtil.getAccount();
        try (var in = osuMapDownloadUtil.download(sid, account)) {
            var zip = new ZipArchiveInputStream(in);
            ZipEntry zipFile;
            Files.createDirectories(tmp);
            while ((zipFile = zip.getNextZipEntry()) != null) {
                if (zipFile.isDirectory()) continue;
                try {
                    Path zipFilePath = Path.of(tmp.toString(), zipFile.getName());
                    Files.write(zipFilePath, zip.readNBytes((int) zipFile.getSize()));
                    fileMap.put(zipFile.getName(), zipFilePath);
                } catch (IOException e) {
                    // do nothing
                    continue;
                }
            }
        }
        if (fileMap.isEmpty()) {
            return false;
        }

        try {
            List<Path> osuPaths = fileMap.entrySet().stream().filter(e -> e.getKey().endsWith(".osu")).map(Map.Entry::getValue).toList();
            dealOsuFiles(osuPaths, fileMap, tmp, sid);
        } catch (Exception e) {
            log.error("处理文件出错:", e);
            deleteDirTree(tmp);
        }
        return true;
    }

    private void dealOsuFiles(List<Path> allOsuFiles, HashMap<String, Path> fileMap, Path saveDir, Long sid) throws IOException {
        List<Path> osuPaths = allOsuFiles;
        List<String> saveFiles = new ArrayList<>(fileMap.size());
        List<BeatMapFileLite> beatmaps = new LinkedList<>();
        for (var osuPath : osuPaths) {
            var info = OsuFile.parseInfo(new BufferedReader(new FileReader(osuPath.toString())));
            info.setSid(sid);
            if (!saveFiles.contains(info.getBackground())) {
                saveFiles.add(info.getBackground());
            }
            if (!saveFiles.contains(info.getAudio())) {
                saveFiles.add(info.getAudio());
            }
            Files.move(osuPath, Path.of(saveDir.toString(), info.getBid() + ".osu"));
            beatmaps.add(info);
        }

        fileMap.forEach((fname, fpath) -> {
            if (saveFiles.contains(fname)) return;
            try {
                Files.delete(fpath);
            } catch (IOException e) {
                return;
            }
        });

        Files.move(saveDir, Path.of(fileConfig.getOsuFilePath(), Long.toString(sid)));
        beatMapFileRepository.saveAllAndFlush(beatmaps);
    }

    public static void deleteDirTree(Path path) {
        try {
            FileSystemUtils.deleteRecursively(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*************************************
     * 下载replay文件 字节文件
     * @param mode
     * @param id
     * @return
     */

    public byte[] getReplay(String mode, long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl("scores/" + mode + "/" + id + "/download")
//                .queryParam("mode","osu")
                .build().encode().toUri();
        HttpHeaders headers = null;

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<byte[]> c = null;
        try {
            System.out.println(uri);
            c = template.exchange(uri, HttpMethod.GET, httpEntity, byte[].class);
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
        return c.getBody();
    }





    public JsonNode chatGetChannels(BinUser user) {
        HashMap body = new HashMap<>();
        body.put("target_id", 7003013);
        body.put("message", "osuMode.getModeValue()");
        body.put("is_action", false);
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJson(body), null);
        var res = template.exchange("/chat/new", HttpMethod.POST, httpEntity, JsonNode.class);
        return res.getBody();
    }

}
