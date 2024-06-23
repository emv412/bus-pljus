import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.buspljus.Adapteri.RVSveLinije
import com.buspljus.R
import com.buspljus.SQLcitac
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class RedVoznjeSveLinije(private val context: Context) {

    private val dialog = BottomSheetDialog(context)
    private lateinit var rvRecyclerView: RecyclerView

    val l = SQLcitac(context).sveLinije()

    fun nacrtajDugmad() {
        with(dialog) {
            setContentView(R.layout.redvoznje_spisakln)
            rvRecyclerView = findViewById(R.id.reciklaza)!!
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
            show()
        }

        rvRecyclerView.layoutManager = GridLayoutManager(context, 5)
        rvRecyclerView.adapter = RVSveLinije(context, l)
    }
}
