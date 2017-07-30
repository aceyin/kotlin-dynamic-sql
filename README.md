# kotlin-dynamic-sql

## about the project

This project is targeting to building dynamic sqls using kotlin scripting language, just like Mybatis XML configuration did.

for example, bellow is a SQL configuration file: user-sql.kts

```
sql {
    val common_cols = "id,name,nick_name,email,avatar,age,status,gender"
    
    "user login" {
        """
        select $common_cols from `user` where username = :username and password = :password
        """
    }
    
    // use "choose-when-else" syntax to build dynamic sql
    "find user by conditions" {
        """
        select $common_cols from `user`
        where 1=1 
        ${+choose When has("nick_name") then {
            "and nick_name like :nickName"
        } When has("gender") then {
            "and gender = :gender"
        } When True("@age>0 and @age<120") then {
            "and age = :age"
        }}
        and status = 1
        """
    }
    
    // use "if-else" syntax to build dynamic sql
    "find user by email" {
        """
        select $common_cols from `user`
        where 1=1
        ${+"and email=:email" If NotNull("email")}
        and status = 1
        """
    }
}
```

then we can use ```sql.build("sql-name",sqlParameterMap)``` to build SQLs dynamically, the program will create a SQL according to the given parameters.

for example:

```
val params = mapOf("nick_name" to "Joe Mark S","age" to 121)
val search_user = sql.build("find user by conditions",params)
```

the above codes generates a SQL like this:
```
select id,name,nick_name,email,avatar,age,status,gender from `user`
where 1=1
and nick_name like :nick_name
and status = 1
```

## the background idea
the project use kotlin scripting language (.kts file) as the SQL configuration file.

when project starting, a ```SqlScriptLoader``` will loading all ".kts" file, and then use ```ScriptEngineManager``` to parse and evaluate them one by one.

during evaluating the ```.kts``` file, the SQLs will be translated into ```Spring EL``` template, and when we call ```sql.build``` method, the program will use ```spring-el``` parser to parse the SQL template into target SQL text.
