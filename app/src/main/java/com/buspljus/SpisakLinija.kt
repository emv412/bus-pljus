import android.content.Context
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.buspljus.Adapteri.SpisakLinijaAdapter
import com.buspljus.Baza.Linije
import com.buspljus.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText

class SpisakLinija(private val context: Context) {

    val dialog = BottomSheetDialog(context)
    private lateinit var rvRecyclerView: RecyclerView
    private lateinit var pretraga: TextInputEditText

    var l = Linije(context).sveLinije("")

    fun nacrtajDugmad() {
        with(dialog) {
            setContentView(R.layout.redvoznje_spisakln)
            rvRecyclerView = findViewById(R.id.reciklaza)!!
            pretraga = findViewById(R.id.pretragaln)!!
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
            show()
        }

        rvRecyclerView.layoutManager = GridLayoutManager(context, 5)
        rvRecyclerView.adapter = SpisakLinijaAdapter(context, l)

        pretraga.doOnTextChanged { text, _, _, _ ->
            l = if (text?.length!! > 0)
                Linije(context).sveLinije(text.toString())
            else
                Linije(context).sveLinije("")
            rvRecyclerView.adapter = SpisakLinijaAdapter(context, l)
        }
    }
}
