package cn.gmlee.plugin.entity;

import lombok.Data;

/**
 * 回调实体类
 *
 * @author Jas°
 * @version 1.0.0
 * @since 2018/07/17 13:10
 */
@Data
public class Callback {
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 保存路径
     */
    private String savePath;
    /**
     * 是否重新格式化代码
     */
    private boolean reformat = true;
}
