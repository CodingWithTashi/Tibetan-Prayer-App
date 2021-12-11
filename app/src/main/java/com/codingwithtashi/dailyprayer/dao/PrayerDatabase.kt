package com.codingwithtashi.dailyprayer.dao

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codingwithtashi.dailyprayer.DataSourceContent
import com.codingwithtashi.dailyprayer.DataSourceDownloadUrl
import com.codingwithtashi.dailyprayer.DataSourceTitle
import com.codingwithtashi.dailyprayer.di.ApplicationScope
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.model.PrayerNotification
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.codingwithtashi.dailyprayer.utils.PrayerPreference
import com.codingwithtashi.dailyprayer.utils.PreferenceConst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Prayer::class,PrayerNotification::class],version = 2,exportSchema = false)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao;
    abstract fun notificationDao(): NotificationDao;
    class Callback @Inject constructor (private val database:Provider<PrayerDatabase>,var context: Context,
    @ApplicationScope private val applicationScope:CoroutineScope):RoomDatabase.Callback(){
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val dao =database.get().prayerDao()
            val notificationDao =database.get().notificationDao()
            applicationScope.launch {
                /*val file_name = "prayer1.sql"
                val prayer1 = context.assets.open(file_name).bufferedReader().use{
                    it.readText()
                }*/
                Log.e("TAG", "onCreate: INSERTED" )
                dao.insert(Prayer(null,DataSourceTitle.GOM_GYAB_DOT_TANG_TITLE,DataSourceContent.GOM_GYAB_DOT_TANG_CONTENT,"url",false,0))
                dao.insert(Prayer(null,DataSourceTitle.KULONG_JAY_TANG_TITLE,DataSourceContent.KULONG_JAY_TANG_CONTENT,"url",false,0,false))

                dao.insert(Prayer(null,DataSourceTitle.GYUN_CHAK_SUM_TITLE,DataSourceContent.GYUN_CHAK_SUM_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.GYUN_CHAK_SUM_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.KYAB_DO_SAM_KAY_TITLE,DataSourceContent.KYAB_DO_SAM_KAY_CONTENT,"url",false,0,false))
               // dao.insert(Prayer(null,DataSourceTitle,"","url",false,0))
                dao.insert(Prayer(null,DataSourceTitle.YANLAK_DUNPA_TITLE,DataSourceContent.YANLAK_DUNPA_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.YANLAK_DUNPA_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.KAP_SUMPA_TITLE,DataSourceContent.KAP_SUMPA_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.KAP_SUMPA_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.KHA_NYAM_MA_TITLE,DataSourceContent.KHA_NYAM_MA_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.KHA_NYAM_MA_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.TSA_SUM_KUN_DU_TITLE,DataSourceContent.TSA_SUM_KUN_DU_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.TSA_SUM_KUN_DU_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.GYALWAY_JABTEN_TITLE,DataSourceContent.GYALWAY_JABTEN_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.GYALWAY_JABTEN_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.GANGLO_MA_TITLE,DataSourceContent.GANGLO_MA_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.GANGLO_MA_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.NGOWA_MONLAM_ONE_TITLE,DataSourceContent.NGOWA_MONLAM_ONE_CONTENT,"url",false,0,false,"",""))
                //Part 2
                dao.insert(Prayer(null,DataSourceTitle.DOLMA_TITLE,DataSourceContent.DOLMA_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.DOLMA_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.SHERAB_NYINGPO_TITLE,DataSourceContent.SHERAB_NYINGPO_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.SHERAB_NYINGPO_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.LHAMO_YANLAK_TITLE,DataSourceContent.LHAMO_YANLAK_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.LHAMO_YANLAK_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.NAYCHUNG_THINLAY_TITLE,DataSourceContent.NAYCHUNG_THINLAY_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.NAYCHUNG_THINLAY_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.BOD_CHOT_YAYUNG_TITLE,DataSourceContent.BOD_CHOT_YAYUNG_CONTENT,"url",false,0,false))
                dao.insert(Prayer(null,DataSourceTitle.THUPTEN_RINMAT_TITLE,DataSourceContent.THUPTEN_RINMAT_CONTENT,"url",false,0,false))
                dao.insert(Prayer(null,DataSourceTitle.DHENTSIG_MONLAM_TITLE,DataSourceContent.DHENTSIG_MONLAM_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.DHENTSIG_MONLAM_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.NGOWA_MONLAM_TWO_TITLE,DataSourceContent.NGOWA_MONLAM_TWO_CONTENT,"url",false,0,false))

                dao.insert(Prayer(null,DataSourceTitle.PHAKTOT_TITLE,"",DataSourceContent.PHAKTOT_CONTENT,false,0,false,"",DataSourceDownloadUrl.PHAKTOT_DONWLOAD_URL));
                dao.insert(Prayer(null,DataSourceTitle.KYABDO_TITLE,DataSourceContent.KYABDO_CONTENT,"",false,0,false,"",DataSourceDownloadUrl.KYABDO_DONWLOAD_URL));
                dao.insert(Prayer(null,DataSourceTitle.BARCHAT_LAMSEL_TITLE,DataSourceContent.BARCHAT_LAMSEL_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.BARCHAT_LAMSEL_DONWLOAD_URL))
                dao.insert(Prayer(null,DataSourceTitle.SAMPA_LUNDUP_TITLE,DataSourceContent.SAMPA_LUNDUP_CONTENT,"url",false,0,false,"",DataSourceDownloadUrl.SAMPA_LUNDUP_DONWLOAD_URL))
                val pref = PrayerPreference.getPreference().getBoolean(PreferenceConst.IS_FIRST_NOTIFICATION_DISPLAYED,false);
                if(!pref){
                    PrayerPreference.getPreference().edit().putBoolean(PreferenceConst.IS_FIRST_NOTIFICATION_DISPLAYED,true).apply()
                    val prayerNotification = PrayerNotification(null,"Hello There","Welcome to Tibetan Prayer,Thanks for using our app. Swipe left to delete notification.",
                        CommonUtils.formatDateFromDate(Date()),"")
                    notificationDao.insert(prayerNotification)
                }

            }
        }

    }
}