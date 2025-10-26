package dbu.models;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StorageFileInfo {
    private String name;
    private long size;
    private LocalDateTime lastModified;
}

