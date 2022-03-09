package com.simplestocklookup.stocklookup

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TableLayout
import com.simplestocklookup.stocklookup.R.id
import com.simplestocklookup.stocklookup.R.layout.activity_stock_lookup_home
import com.squareup.picasso.Picasso
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


class StockLookupHome : AppCompatActivity() {

    private var linnServer: String? = null
    private var linnToken: String? = null
    private var linnList: JSONArray? = null

    //Linnworks Auth Details
    private fun getLinnAuth() {
        doAsync {
            val httpcon: HttpURLConnection
            val url: String? = "https://api.linnworks.net//api/Auth/AuthorizeByApplication"
            val data: String? = "applicationId=5e98796e-63fb-4188-a73b-0b05456f081e&applicationSecret=ad4c386b-d125-4961-9f3c-36340dd33753&token=14744072963ee9ace93725ca8a8c70b2"
            httpcon = URL(url).openConnection() as HttpURLConnection

            try {
                //Connect

                httpcon.doOutput = true
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                httpcon.setRequestProperty("Accept", "application/json")
                httpcon.requestMethod = "POST"
                httpcon.connect()

                //Write
                val os = httpcon.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.close()
                os.close()

                //Read
                val br = BufferedReader(InputStreamReader(httpcon.inputStream, "UTF-8"))
                var line: String?
                val sb = StringBuilder()

                do {
                    line = br.readLine()
                    sb.append(line)
                } while (line != null)

                br.close()
                val result = sb.toString()
                val obj = JSONObject(result)
                linnServer = obj.getString("Server")
                Log.d("Server", obj.getString("Server"))
                linnToken = obj.getString("Token")
                Log.d("Token", obj.getString("Token"))
                uiThread {
                    val res = linnToken
                    if(res == null){toast("Auth Failed")}
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally{
                httpcon.disconnect()
                getLinnDB()
            }

        }
    }

    private fun getLinnDB() {
        doAsync {
            val httpcon: HttpURLConnection
            val url = "$linnServer//api/Dashboards/ExecuteCustomScriptQuery"
            val data = "script= SELECT ItemNumber, pkStockItemID FROM StockItem WHERE bLogicalDelete = 0 ORDER BY ItemNumber"
            Log.d("URL",url)
            Log.d("Data",data)
            httpcon = URL(url).openConnection() as HttpURLConnection
            try {
                //Connect
                httpcon.doOutput = true
                httpcon.setRequestProperty("Authorization", linnToken)
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                httpcon.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01")
                httpcon.requestMethod = "POST"
                httpcon.connect()

                //Write
                val os = httpcon.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.close()
                os.close()

                val statusCode = httpcon.responseCode
                Log.d("status",statusCode.toString())

                //Read
                val br = BufferedReader(InputStreamReader(httpcon.inputStream, StandardCharsets.UTF_8))

                var line: String?
                val sb = StringBuilder()

                do {
                    line = br.readLine()
                    sb.append(line)
                } while (line != null)

                br.close()
                val result = sb.toString()

                val obj = JSONObject(result)
                val arr = obj.getJSONArray("Results")
                uiThread {
                    toast("Connected")
                    linnList = arr
                    Log.d("List",linnList.toString())

                    val skuList = ArrayList<String>()

                    for (i in 0 until linnList!!.length()) {
                        val e = linnList!!.getJSONObject(i)
                        val sku = e.getString("ItemNumber")
                        skuList.add(sku)
                    }

                    val autoCompleteTextView = findViewById<AutoCompleteTextView>(id.AutoCompleteTextView)
                    // Create the adapter and set it to the MultiAutoCompleteTextView
                    val adapter = ArrayAdapter<String>(this@StockLookupHome,android.R.layout.simple_list_item_1,skuList)//this, android.R.layout.simple_list_item_1, skuList)
                    autoCompleteTextView.setAdapter(adapter)
                    autoCompleteTextView.threshold = 1

                    autoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
                        val selectedProduct = autoCompleteTextView.text.toString()
                        Log.d("Selected!", selectedProduct)
                        for (i in 0 until linnList!!.length()) {
                            @Suppress("NAME_SHADOWING")
                            val obj = linnList!!.getJSONObject(i)
                            val comp = obj.get("ItemNumber")
                            if(comp == selectedProduct) {
                                val selectedLinnItem = comp.toString()
                                val selectedLinnId = obj.get("pkStockItemID").toString()
                                Log.d("found item!", "$selectedLinnItem | $selectedLinnId")
                                getLinnItem(selectedLinnId)
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
                            }
                        }
                    }
                }

            }
            catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }finally {
                httpcon.disconnect()
            }

        }
    }

    private fun getLinnItem(id:String) {
        doAsync {
            val httpcon: HttpURLConnection
            val url = "$linnServer//api/Inventory/GetInventoryItemById"
            val data = "Id=$id"
            Log.d("URL", url)
            Log.d("Data", data)
            httpcon = URL(url).openConnection() as HttpURLConnection
            try {
                //Connect
                httpcon.doOutput = true
                httpcon.setRequestProperty("Authorization", linnToken)
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                httpcon.setRequestProperty("Accept", "application/json, text/plain, */*; q=0.01")
                httpcon.requestMethod = "POST"
                httpcon.connect()

                //Write
                val os = httpcon.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.close()
                os.close()

                val statusCode = httpcon.responseCode
                Log.d("status", statusCode.toString())

                //Read
                val br = BufferedReader(InputStreamReader(httpcon.inputStream, StandardCharsets.UTF_8))

                var line: String?
                val sb = StringBuilder()

                do {
                    line = br.readLine()
                    sb.append(line)
                } while (line != null)

                br.close()
                val result = sb.toString()
                Log.d("Item",result)
                val obj = JSONObject(result)
                uiThread {
                    val isComp = obj.get("IsCompositeParent")
                    Log.d("isComp",isComp.toString())
                    if(isComp.toString() == "true"){
                        getLinnComp(id)
                    }
                    else{
                        getLinnPhoto(id){
                            returnString -> Log.d("Result",returnString)

                            val mainTable = findViewById<TableLayout>(R.id.mainTable)
                            mainTable.removeAllViews()

                            val itemSKU = obj.getString("ItemNumber")
                            val itemTitle = obj.getString("ItemTitle")
                            val itemQty = "1"

                            val row = TableRow(this@StockLookupHome)
                            val lp = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                                    TableLayout.LayoutParams.WRAP_CONTENT)
                            lp.setMargins(5,5,5,5)
                            row.setBackgroundColor(Color.WHITE)
                            row.layoutParams = lp
                            row.setBackgroundResource(R.drawable.item_bg_rc)
                            val iv1 = ImageView(this@StockLookupHome)
                            Picasso.get().load(returnString).resize(0, 100).into(iv1)
                            val tv1 = TextView(this@StockLookupHome)
                            tv1.text = itemSKU
                            tv1.width = findViewById<TextView>(R.id.trSKU).measuredWidth
                            tv1.height = 100
                            val tv2 = TextView(this@StockLookupHome)
                            tv2.text = itemTitle
                            tv2.width = findViewById<TextView>(R.id.trTitle).measuredWidth
                            tv2.height = 100
                            val tv3 = TextView(this@StockLookupHome)
                            tv3.text = itemQty
                            tv3.width = findViewById<TextView>(R.id.trQty).measuredWidth
                            tv3.height = 100
                            row.addView(iv1)
                            row.addView(tv1)
                            row.addView(tv2)
                            row.addView(tv3)

                            mainTable.addView(row)
                        }
                    }
                }
            }
            catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }finally {
                httpcon.disconnect()
            }
        }
    }

    private fun getLinnComp(id:String) {
        doAsync {
            val httpcon: HttpURLConnection
            val url = "$linnServer//api/Inventory/GetInventoryItemCompositions"
            val data = "inventoryItemId=$id"
            Log.d("URL", url)
            Log.d("Data", data)
            httpcon = URL(url).openConnection() as HttpURLConnection
            try {
                //Connect
                httpcon.doOutput = true
                httpcon.setRequestProperty("Authorization", linnToken)
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                httpcon.setRequestProperty("Accept", "application/json, text/plain, */*; q=0.01")
                httpcon.requestMethod = "POST"
                httpcon.connect()

                //Write
                val os = httpcon.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.close()
                os.close()

                val statusCode = httpcon.responseCode
                Log.d("status", statusCode.toString())

                //Read
                val br = BufferedReader(InputStreamReader(httpcon.inputStream, StandardCharsets.UTF_8))

                var line: String?
                val sb = StringBuilder()

                do {
                    line = br.readLine()
                    sb.append(line)
                } while (line != null)

                br.close()
                val result = sb.toString()
                Log.d("Item",result)
                val obj = JSONArray(result)
                uiThread {

                    val mainTable = findViewById<TableLayout>(R.id.mainTable)
                    mainTable.removeAllViews()

                    for (i in 0 until obj.length()) {
                        val e = obj.getJSONObject(i)
                        val itemSKU = e.getString("SKU")
                        val itemTitle = e.getString("ItemTitle")
                        val itemQty = e.getString("Quantity")
                        val itemId = e.getString("LinkedStockItemId")

                        getLinnPhoto(itemId) { returnString ->
                            Log.d("Result", returnString)

                            val row = TableRow(this@StockLookupHome)

                            val lp = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                                    TableLayout.LayoutParams.WRAP_CONTENT)
                            lp.setMargins(5,5,5,5)
                            if(itemQty.toInt() > 1){row.setBackgroundColor(Color.RED)}
                            else{row.setBackgroundColor(Color.WHITE)}
                            row.layoutParams = lp
                            row.setBackgroundResource(R.drawable.item_bg_rc)
                            val iv1 = ImageView(this@StockLookupHome)
                            Picasso.get().load(returnString).resize(0, 100).into(iv1)
                            iv1.maxHeight = 100
                            val tv1 = TextView(this@StockLookupHome)
                            tv1.text = itemSKU
                            tv1.width = findViewById<TextView>(R.id.trSKU).measuredWidth
                            tv1.height = 100
                            val tv2 = TextView(this@StockLookupHome)
                            tv2.text = itemTitle
                            tv2.width = findViewById<TextView>(R.id.trTitle).measuredWidth
                            tv2.height = 100
                            val tv3 = TextView(this@StockLookupHome)
                            tv3.text = itemQty
                            tv3.width = findViewById<TextView>(R.id.trQty).measuredWidth
                            tv3.height = 100
                            row.addView(iv1)
                            row.addView(tv1)
                            row.addView(tv2)
                            row.addView(tv3)

                            mainTable.addView(row)
                        }
                    }
                }

            }
            catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }finally {
                httpcon.disconnect()
            }
        }
    }

    private fun getLinnPhoto(id:String, callback: (String) -> Unit){
        doAsync {
            val httpcon: HttpURLConnection
            val url: String? = "$linnServer//api/Inventory/GetInventoryItemImages"
            val data: String? = "inventoryItemId=$id"
            httpcon = URL(url).openConnection() as HttpURLConnection
            try {
                //Connect
                httpcon.doOutput = true
                httpcon.setRequestProperty("Authorization", linnToken)
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                httpcon.setRequestProperty("Accept", "application/json, text/plain, */*; q=0.01")
                httpcon.requestMethod = "POST"
                httpcon.connect()

                //Write
                val os = httpcon.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(data)
                writer.close()
                os.close()

                val statusCode = httpcon.responseCode
                Log.d("status", statusCode.toString())

                //Read
                val br = BufferedReader(InputStreamReader(httpcon.inputStream, StandardCharsets.UTF_8))

                var line: String?
                val sb = StringBuilder()

                do {
                    line = br.readLine()
                    sb.append(line)
                } while (line != null)

                br.close()
                val result = sb.toString()
                Log.d("Result",result)
                val arr = JSONArray(result)
                if(arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    uiThread {
                        val returnString = obj.getString("FullSource")
                        callback(returnString)
                    }
                }
                else{
                    uiThread {
                        val returnString = "https://i.stack.imgur.com/8cenv.png"
                        callback(returnString)
                    }
                }
            }
            catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }finally {
                httpcon.disconnect()
            }
        }
    }

    fun resetText(@Suppress("UNUSED_PARAMETER")view:View) {
        val mainTable = findViewById<TableLayout>(id.mainTable)
        mainTable.removeAllViews()
        val autoText = findViewById<TextView>(id.AutoCompleteTextView)
        autoText.text = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_stock_lookup_home)

    }

    public override fun onResume() {
        super.onResume()
        getLinnAuth()
    }
}

