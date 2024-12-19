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
