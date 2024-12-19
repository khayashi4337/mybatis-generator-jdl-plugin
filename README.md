# MyBatis Generator JDL プラグイン

既存のデータベーススキーマからJHipster Domain Language（JDL）ファイルを生成するMyBatis Generatorプラグインです。このプラグインは、MyBatisベースのアプリケーションからJHipsterアプリケーションへの移行を容易にします。

## 機能

- データベーステーブルからJDLエンティティを自動生成
- エンティティ間の関係を検出して生成
- データベース制約をJDLバリデーションルールに変換
- カスタムバリデーションルールをサポート
- データベース型を適切なJDL型にマッピング
- テーブルコメントからJHipster互換のエンティティ説明を生成
- データベースインデックスに基づいてJHipsterの設定を最適化

## クイックスタート

### 1. 依存関係の追加

Mavenプロジェクトの`pom.xml`に以下の依存関係を追加します：

```xml
<dependency>
    <groupId>com.example.mybatis</groupId>
    <artifactId>mybatis-generator-jdl-plugin</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. データベーススキーマの準備

以下は、ブログシステムを例としたサンプルスキーマです：

```sql
-- ユーザーテーブル
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'ユーザー名（一意）',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'メールアドレス（一意）',
    password_hash VARCHAR(255) NOT NULL COMMENT 'パスワードハッシュ',
    first_name VARCHAR(50) COMMENT '名',
    last_name VARCHAR(50) COMMENT '姓',
    birth_date DATE COMMENT '生年月日',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 投稿テーブル
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '投稿者ID',
    title VARCHAR(200) NOT NULL COMMENT '投稿タイトル',
    content TEXT COMMENT '投稿内容',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 3. MyBatis Generator設定

`generatorConfig.xml`ファイルを作成し、以下のように設定します：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">

<generatorConfiguration>
    <context id="MySQLTables" targetRuntime="MyBatis3">
        <!-- JDLプラグインの設定 -->
        <plugin type="com.example.mybatis.JDLGeneratorPlugin">
            <property name="outputPath" value="src/main/resources/app.jdl"/>
            <property name="detectRelationships" value="true"/>
            <property name="generateValidations" value="true"/>
        </plugin>

        <!-- データベース接続設定 -->
        <jdbcConnection driverClass="com.mysql.cj.jdbc.Driver"
                        connectionURL="jdbc:mysql://localhost:3306/blog_db"
                        userId="root"
                        password="root">
        </jdbcConnection>

        <!-- 必要なジェネレーター設定 -->
        <javaModelGenerator targetPackage="com.example.model"
                           targetProject="src/main/java">
        </javaModelGenerator>

        <!-- テーブル設定 -->
        <table tableName="users" domainObjectName="User"/>
        <table tableName="posts" domainObjectName="Post"/>
    </context>
</generatorConfiguration>
```

### 4. プラグインの実行

以下のMavenコマンドを実行してJDLファイルを生成します：

```bash
mvn mybatis-generator:generate
```

### 5. 生成されるJDLファイル

プラグインは以下のようなJDLファイルを生成します：

```jdl
entity User {
    username String required unique maxlength(50)
    email String required unique maxlength(100)
    passwordHash String required maxlength(255)
    firstName String maxlength(50)
    lastName String maxlength(50)
    birthDate LocalDate
    createdAt Instant required
    updatedAt Instant required
}

entity Post {
    title String required maxlength(200)
    content TextBlob
    status String required maxlength(20)
    createdAt Instant required
}

// リレーションシップ
relationship OneToMany {
    User{posts} to Post
}

// バリデーションルール
@UniqueConstraint("username")
@UniqueConstraint("email")
```

## 使用方法

MyBatis Generatorの設定にプラグインを追加してください：

```xml
<plugin type="com.example.mybatis.JDLGeneratorPlugin">
    <property name="outputPath" value="src/main/resources/app.jdl"/>
    <property name="detectRelationships" value="true"/>
    <property name="generateValidations" value="true"/>
</plugin>
```

## 出力例

プラグインは以下のようなJDLファイルを生成します：

```jdl
entity User {
    id Long,
    name String required,
    email String required unique,
    createdAt Instant,
    updatedAt Instant
}

// 関係性は自動的に検出されます
relationship OneToMany {
    User{posts} to Post
}

// データベース制約から導出されたバリデーションルール
@UniqueConstraint("email")
@Column(name="created_at")
```

## メリット

- 既存のデータベースからJHipsterへの容易な移行パス
- 双方向変換機能（DB → JDL → JHipsterアプリケーション）
- 既存のSpring Boot + MyBatisアプリケーションの段階的な移行が可能
- 既存のデータベーススキーマを活用して最新のJHipsterアプリケーションを生成

## 設定プロパティ

| プロパティ | 説明 | デフォルト値 |
|------------|------|--------------|
| outputPath | JDLファイルが生成されるパス | src/main/resources/app.jdl |
| detectRelationships | リレーションシップ検出の有効/無効 | true |
| generateValidations | バリデーションルール生成の有効/無効 | true |

## コントリビューション

コントリビューションを歓迎します！お気軽にプルリクエストを送信してください。

## ライセンス

このプロジェクトはMITライセンスの下で提供されています - 詳細はLICENSEファイルをご覧ください。

## 今後の拡張予定

- データベースインデックス情報からJHipsterのパフォーマンス最適化設定の生成
- テーブルコメントからのエンティティ説明の生成
- カスタムバリデーションルールの変換
- マイクロサービス設定の提案
