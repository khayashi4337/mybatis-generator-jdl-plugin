# MyBatis Generator JDL Plugin サンプルプロジェクト

このサンプルプロジェクトは、MyBatis Generator JDL Pluginの使用方法を示すためのものです。
ブログシステムのデータベーススキーマを例として、JHipster Domain Language（JDL）ファイルの生成方法を説明します。

## 前提条件

- Java 8以上
- Maven 3.6以上
- Docker と Docker Compose

## セットアップ手順

1. MySQLデータベースの準備：

```bash
# MySQLコンテナの起動（初回起動時にスキーマが自動的に作成されます）
docker-compose up -d

# コンテナのステータス確認
docker-compose ps

# ログの確認（必要な場合）
docker-compose logs mysql
```

2. プラグインのインストール：

```bash
cd ..
mvn clean install
cd sample
```

3. JDLファイルの生成：

```bash
mvn mybatis-generator:generate
```

生成されたJDLファイルは `src/main/resources/app.jdl` に出力されます。

## プロジェクト構成

- `pom.xml`: Mavenプロジェクト設定
- `src/main/resources/`
  - `schema.sql`: サンプルデータベーススキーマ
  - `generatorConfig.xml`: MyBatis Generator設定
  - `app.jdl`: 生成されるJDLファイル（実行後に作成）

## データモデル

### Users（ユーザー）
- ユーザー名（一意）
- メールアドレス（一意）
- パスワードハッシュ
- 名前（姓・名）
- 生年月日
- タイムスタンプ

### Posts（投稿）
- タイトル
- 内容
- ステータス（下書き、公開、アーカイブ）
- 閲覧数
- タイムスタンプ

### Comments（コメント）
- 内容
- タイムスタンプ
- 投稿とユーザーへの参照

## 生成されるJDLファイルの特徴

- エンティティ定義
- リレーションシップ（OneToMany）
- バリデーションルール
  - 必須フィールド
  - 文字列の長さ制限
  - 一意性制約
- フィールドのコメント
