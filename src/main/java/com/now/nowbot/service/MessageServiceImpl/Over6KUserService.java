package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service("FOR_NEWBIE_GROUP_WEB_API")
public class Over6KUserService implements MessageService<Over6KUserService.OverUser> {
    private static File DATA_FILE;
    private static boolean INITED = false;
    @Resource
    OsuUserApiService userApiService;
    public Over6KUserService(FileConfig fileConfig) throws IOException {
        DATA_FILE = new File(fileConfig.getRoot(), "6k.out");
        try {
            if (! DATA_FILE.exists()) {
                DATA_FILE.createNewFile();
            }
        } catch (IOException err) {
            INITED = false;
            return;
        }

        INITED = true;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<OverUser> data) throws Throwable {
        if (INITED && ! messageText.startsWith("高阶出群")) {
            return false;
        }
        var ps = messageText.split("#");
        if (ps.length < 3) return false;
        var name = ps[1];
        LocalDate time;
        try {
            time = LocalDate.parse(ps[2]);
        } catch (Exception e) {
            throw new TipsException("日期格式错误, 必须为 yyyy-MM-dd");
        }
        Long id;
        try {
            id = userApiService.getOsuId(name);
        } catch (Exception e) {
            throw new TipsException(STR."没找到用户\{name}");
        }

        data.setValue(new OverUser(id, name, time));
        return true;
    }

    @CheckPermission(isSuperAdmin = true)
    @Override
    public void HandleMessage(MessageEvent event, OverUser user) throws Throwable {
        saveUser(user);
        event.getSubject().sendMessage("添加成功");
    }

    public record OverUser(long id, String name, LocalDate date) {
    }

    private RandomAccessFile readFile() throws FileNotFoundException {
        return new RandomAccessFile(DATA_FILE, "r");
    }

    private RandomAccessFile writeFile() throws FileNotFoundException {
        return new RandomAccessFile(DATA_FILE, "rws");
    }

    private List<OverUser> getUsers(RandomAccessFile data, int size) throws IOException {
        return getUsers(data, 0, size);
    }

    private List<OverUser> getUsers(RandomAccessFile data, int start, int size) throws IOException {
        while (start > 0) {
            skipUser(data);
            start--;
        }
        var result = new ArrayList<OverUser>();
        for (int i = 0; i < size; i++) {
            readUser(data).ifPresent(result::add);
        }
        return result;
    }

    private void skipUser (RandomAccessFile data) throws IOException {
        int dataSize = data.readInt() - 4;
        if (data.getFilePointer() + dataSize < data.length()) {
            data.seek(dataSize);
        }
    }

    private Optional<OverUser> readUser (RandomAccessFile data) throws IOException {
        if (data.length() - data.getFilePointer() < 4) {
            return Optional.empty();
        }
        int dataSize = data.readInt() - 4;
        if (data.getFilePointer() + dataSize > data.length() || dataSize <= 18) {
            return Optional.empty();
        }
        long id = data.readLong();
        byte[] date = new byte[dataSize - 8];
        data.read(date);
        String dateStr = new String(date, 0, 10);
        String name = new String(date, 10, dataSize - 18);
        return Optional.of(new OverUser(id, name, LocalDate.parse(dateStr)));
    }

    private byte[] getUserData(OverUser u) {
        byte[] name = u.name.getBytes();
        //  (size)4 + (uid)8 + (data)10 + name
        int size = 22 + name.length;
        ByteBuffer data = ByteBuffer.allocate(size);
        data.putInt(size);
        data.putLong(u.id);
        data.put(u.date.toString().getBytes());
        data.put(name);
        return data.array();
    }

    private void saveUser(long id, String name, String timeStr) throws IOException {
        System.out.println("save "+id);
        try (var f = writeFile();) {
            var u = new OverUser(id, name, LocalDate.parse(timeStr));
            var d = getUserData(u);
            f.seek(f.length());
            f.write(d);
        }
    }

    private void saveUser(OverUser u) throws IOException {
        try (var f = writeFile();) {
            var d = getUserData(u);
            f.seek(f.length());
            f.write(d);
        }
    }

    public String getResultJson(int start, int size) throws IOException {
        var d = getUsers(readFile(), start, size);
        return JacksonUtil.toJson(d);
    }


}
