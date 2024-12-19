package com.example.mybatis;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * JHipster Domain Language (JDL) ファイルを生成するMyBatis Generatorプラグイン。
 * このプラグインは、既存のデータベーススキーマからJDLファイルを自動生成し、
 * MyBatisベースのアプリケーションからJHipsterアプリケーションへの移行を支援します。
 *
 * <p>主な機能：</p>
 * <ul>
 *   <li>データベーステーブルからJDLエンティティの自動生成</li>
 *   <li>エンティティ間のリレーションシップの検出と生成</li>
 *   <li>データベース制約からJDLバリデーションルールへの変換</li>
 *   <li>データベース型からJDL型への適切なマッピング</li>
 * </ul>
 *
 * <p>使用例：</p>
 * <pre>
 * {@code
 * <plugin type="com.example.mybatis.JDLGeneratorPlugin">
 *     <property name="outputPath" value="src/main/resources/app.jdl"/>
 *     <property name="detectRelationships" value="true"/>
 *     <property name="generateValidations" value="true"/>
 * </plugin>
 * }
 * </pre>
 *
 * @version 1.0.0
 * @see org.mybatis.generator.api.PluginAdapter
 * @see org.mybatis.generator.api.IntrospectedTable
 */
public class JDLGeneratorPlugin extends PluginAdapter {

    private String outputPath;
    private boolean detectRelationships;
    private boolean generateValidations;
    private final Map<String, JDLEntity> entities;
    private final List<JDLRelationship> relationships;

    /**
     * プラグインのコンストラクタ。
     * エンティティとリレーションシップを保持するためのコレクションを初期化します。
     */
    public JDLGeneratorPlugin() {
        this.entities = new HashMap<>();
        this.relationships = new ArrayList<>();
    }

    /**
     * プラグインの設定を検証し、必要なプロパティを初期化します。
     *
     * @param warnings 警告メッセージのリスト
     * @return 常にtrue（検証は常に成功します）
     */
    @Override
    public boolean validate(List<String> warnings) {
        outputPath = properties.getProperty("outputPath", "src/main/resources/app.jdl");
        detectRelationships = Boolean.parseBoolean(properties.getProperty("detectRelationships", "true"));
        generateValidations = Boolean.parseBoolean(properties.getProperty("generateValidations", "true"));
        return true;
    }

    /**
     * モデルクラスの生成時に呼び出され、テーブル情報からJDLエンティティを生成します。
     *
     * @param topLevelClass 生成されるモデルクラス
     * @param introspectedTable データベーステーブルの情報
     * @return 常にtrue
     */
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        processTable(introspectedTable);
        return true;
    }

    /**
     * テーブル情報を処理し、JDLエンティティを生成します。
     * カラム情報から各フィールドの型、必須制約、一意性制約を抽出します。
     *
     * @param table データベーステーブルの情報
     */
    private void processTable(IntrospectedTable table) {
        JDLEntity entity = new JDLEntity(table.getFullyQualifiedTable().getDomainObjectName());

        // カラムの処理
        for (IntrospectedColumn column : table.getAllColumns()) {
            // 主キーまたは一意制約の確認
            boolean isUnique = column.isIdentity() ||
                    (column.getRemarks() != null && column.getRemarks().toLowerCase().contains("unique"));

            JDLField field = new JDLField(
                    column.getJavaProperty(),
                    mapJDBCTypeToJDLType(column.getJdbcTypeName()),
                    column.isNullable(),
                    isUnique);

            // バリデーションルールの生成
            if (generateValidations) {
                // 文字列の長さ制約
                if (column.getLength() > 0 && "String".equals(field.getType())) {
                    field.addValidation("maxlength", String.valueOf(column.getLength()));
                }
                
                // 数値の範囲制約
                if ("Integer".equals(field.getType()) || "Long".equals(field.getType())) {
                    if (column.getScale() > 0) {
                        field.addValidation("min", "0");
                        field.addValidation("max", String.valueOf(Math.pow(10, column.getScale()) - 1));
                    }
                }

                // 小数点の精度
                if ("BigDecimal".equals(field.getType())) {
                    if (column.getScale() > 0) {
                        field.addValidation("decimal", String.valueOf(column.getScale()));
                    }
                }
            }

            entity.addField(field);
        }

        entities.put(entity.getName(), entity);

        // リレーションシップの処理（有効な場合）
        if (detectRelationships) {
            processRelationships(table);
        }
    }

    /**
     * JDBCデータ型をJDLデータ型に変換します。
     * JHipsterで使用される適切なデータ型にマッピングします。
     *
     * @param jdbcType 変換元のJDBCデータ型
     * @return 対応するJDLデータ型
     */
    private String mapJDBCTypeToJDLType(String jdbcType) {
        switch (jdbcType.toUpperCase()) {
            case "VARCHAR":
            case "CHAR":
            case "LONGVARCHAR":
                return "String";
            case "INTEGER":
            case "SMALLINT":
                return "Integer";
            case "BIGINT":
                return "Long";
            case "DOUBLE":
            case "NUMERIC":
            case "DECIMAL":
                return "BigDecimal";
            case "DATE":
                return "LocalDate";
            case "TIMESTAMP":
                return "Instant";
            case "BOOLEAN":
                return "Boolean";
            default:
                return "String";
        }
    }

    /**
     * テーブル間のリレーションシップを検出し、JDLリレーションシップを生成します。
     * 現在は、命名規則（_id接尾辞）に基づいて外部キーを検出します。
     *
     * @param table データベーステーブルの情報
     */
    private void processRelationships(IntrospectedTable table) {
        // 外部キーを検出してリレーションシップを生成
        for (IntrospectedColumn column : table.getAllColumns()) {
            if (column.isIdentity()) {
                continue;
            }

            String columnName = column.getActualColumnName().toLowerCase();
            if (columnName.endsWith("_id")) {
                String targetTableName = columnName.substring(0, columnName.length() - 3);
                JDLRelationship relationship = new JDLRelationship(
                        "OneToMany",
                        table.getFullyQualifiedTable().getDomainObjectName(),
                        targetTableName,
                        column.getJavaProperty());
                relationships.add(relationship);
            }
        }
    }

    /**
     * SQLマップファイルの生成後に呼び出され、JDLファイルを生成します。
     *
     * @param sqlMap 生成されたXMLファイル
     * @param introspectedTable データベーステーブルの情報
     * @return 常にtrue
     */
    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        // すべてのテーブルの処理後にJDLファイルを生成
        generateJDLFile();
        return true;
    }

    /**
     * 収集した情報からJDLファイルを生成します。
     * エンティティとリレーションシップの定義を指定されたパスに出力します。
     */
    private void generateJDLFile() {
        try (FileWriter writer = new FileWriter(new File(outputPath))) {
            // エンティティの書き込み
            for (JDLEntity entity : entities.values()) {
                writer.write(entity.toString());
                writer.write("\n\n");
            }

            // リレーションシップの書き込み
            if (!relationships.isEmpty()) {
                writer.write("// リレーションシップ\n");
                for (JDLRelationship relationship : relationships) {
                    writer.write(relationship.toString());
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * JDLエンティティを表現する内部クラス。
     * エンティティ名とフィールドのリストを保持します。
     */
    private static class JDLEntity {
        private final String name;
        private final List<JDLField> fields;

        public JDLEntity(String name) {
            this.name = name;
            this.fields = new ArrayList<>();
        }

        public void addField(JDLField field) {
            fields.add(field);
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("entity ").append(name).append(" {\n");
            for (JDLField field : fields) {
                sb.append("    ").append(field.toString()).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * JDLフィールドを表現する内部クラス。
     * フィールド名、型、必須制約、一意性制約の情報を保持します。
     */
    private static class JDLField {
        private final String name;
        private final String type;
        private final boolean required;
        private final boolean unique;
        private final Map<String, String> validations;

        public JDLField(String name, String type, boolean nullable, boolean unique) {
            this.name = name;
            this.type = type;
            this.required = !nullable;
            this.unique = unique;
            this.validations = new LinkedHashMap<>();
        }

        public String getType() {
            return type;
        }

        public void addValidation(String rule, String value) {
            validations.put(rule, value);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" ").append(type);
            
            // 基本制約
            if (required) {
                sb.append(" required");
            }
            if (unique) {
                sb.append(" unique");
            }

            // バリデーションルール
            if (!validations.isEmpty()) {
                sb.append(" @");
                boolean first = true;
                for (Map.Entry<String, String> validation : validations.entrySet()) {
                    if (!first) {
                        sb.append(" @");
                    }
                    sb.append(validation.getKey()).append("(").append(validation.getValue()).append(")");
                    first = false;
                }
            }
            
            return sb.toString();
        }
    }

    /**
     * JDLリレーションシップを表現する内部クラス。
     * リレーションシップの種類、ソースエンティティ、ターゲットエンティティ、
     * フィールド名の情報を保持します。
     */
    private static class JDLRelationship {
        private final String type;
        private final String sourceEntity;
        private final String targetEntity;
        private final String fieldName;

        public JDLRelationship(String type, String sourceEntity, String targetEntity, String fieldName) {
            this.type = type;
            this.sourceEntity = sourceEntity;
            this.targetEntity = targetEntity;
            this.fieldName = fieldName;
        }

        @Override
        public String toString() {
            return String.format("relationship %s {\n    %s{%s} to %s\n}",
                    type, sourceEntity, fieldName, targetEntity);
        }
    }
}
