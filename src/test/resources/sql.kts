import com.aceyin.dynamic.sql.SqlScriptBuilder.False
import com.aceyin.dynamic.sql.SqlScriptBuilder.Null
import com.aceyin.dynamic.sql.SqlScriptBuilder.True
import com.aceyin.dynamic.sql.SqlScriptBuilder.has
import com.aceyin.dynamic.sql.choose
import com.aceyin.dynamic.sql.invoke
import com.aceyin.dynamic.sql.sql
import com.aceyin.dynamic.sql.unaryPlus

sql {
    val shared_order_commons = "SELECT o.id AS id FROM `order` o"

    "get order by id" {
        """
        $shared_order_commons where 1=1
        ${+"and o.status = :status" If has("status")}
        ${+"and o.age = :age and o.gender = :gender" If has("age", "gender")}
        ${+"and o.urgent = :urgent" If True("@urgent == 1")}
        ${+"and o.is_test = :isTest"}
        """
    }

    "find user by condition" {
        """
        select * from user
        where name = :name
        """
    }

    val shared_product_cols = "select * from product p"

    "get product by condition" {
        """
        $shared_product_cols where 1=1
        ${+choose When has("name") then {
            "and p.name like :name"
        } When False("@onSale") then {
            "and p.on_sale = :onSale"
        } Else "and p.status = 1"}
        """
    }

    "search orders" {
        """
        $shared_order_commons where 1=1
        ${+choose When has("name") then { "and o.name = :name" }}
        ${+choose When True("@status == 'payed'") then {
            "and o.status='paied'"
        } When False("@settled == 1") then {
            "and o.settled = 1"
        } When Null("parent") then {
            "and o.parent = null"
        } Else "o.status=1"}
        """
    }
}