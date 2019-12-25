# xin-spring-boot-starter
commit code
# 此项目基于mybatis-plus进行二次开发。自定义注解。用于快速开发SpringCloud微服务,处理数据库表之间关联操作
```java
/**
 * <p>
 * favorite
 * </p>
 *
 * @author Luchaoxin
 * @since 2019-04-16
 */
@ApiModel(value="Favorite对象", description="favorite")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName(value = "favorite")
public class Favorite extends XModel<Favorite> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户id")
    @TableField(fill = FieldFill.INSERT, el = "userInfoId, jdbcType = BIGINT")
    private Long userInfoId;

    @ApiModelProperty(value = "收藏信息id")
    @TableId(value = "id", type = IdType.INPUT)
    @TableField(el = "id, jdbcType = BIGINT")
    private Long id;

    @NotBlank(message = "[patentId]专利id不能为空")
    @ApiModelProperty(value = "专利id")
    @TableField(el = "patentId, jdbcType = VARCHAR")
    private String patentId;

    @ApiModelProperty(value = "收藏名称")
    @TableField(el = "name, jdbcType = VARCHAR")
    private String name;

    @ApiModelProperty(value = "收藏评级")
    @TableField(el = "rateLevel, jdbcType = INTEGER")
    private Integer rateLevel;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE, el = "updateTime, jdbcType = TIMESTAMP")
    private Timestamp updateTime;

    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT, el = "createTime, jdbcType = TIMESTAMP")
    private Timestamp createTime;

    @TableField(exist = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @RelationInfo(middleTable = "favorite_custom_level", relationTable = "favorite", relationToTable = "custom_level")
    private List<CustomLevel> customLevels;

    @TableField(exist = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @RelationInfo(isReachable = false, deleteRelation = false,
            relationTable = "favorite", relationToTable = "user_info")
    private UserInfo userInfo;

    @TableField(exist = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Map<String, Object> patent;

    @TableField(exist = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<CustomLevelTreeNode> treeNodes;

    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}
```
