package com.codingwithtashi.dailyprayer.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.codingwithtashi.dailyprayer.di.ApplicationScope
import com.codingwithtashi.dailyprayer.model.Prayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Prayer::class],version = 1)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao;
    class Callback @Inject constructor (private val database:Provider<PrayerDatabase>,
    @ApplicationScope private val applicationScope:CoroutineScope):RoomDatabase.Callback(){
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val dao =database.get().prayerDao()
            applicationScope.launch {
                dao.insert(Prayer(2,"རྒྱུན་ཆགས་གསུམ་པ།","༄༅། །སྟོ ན་པ་བཅོམ་ལྡན་འདས་དེ་བཞིན་གཤེགས་པ་དགྲ་བཅོམ་ པ་ཡང་དག་པ་རྫོགས་པའི་སངས་རྒྱས་རིག་པ་དང་ཞབས་སུ་ལྡན་པ། བདེ་ ས་ བར་གཤེགས་པ་འཇིག་རྟེན་མཁྱེན་པ། སྐྱེས་བུ་འདུལ་བའི་ཁ་ལོ་བསྒྱུར་ བ། བླ་ན་མེད་པ། ལྷ་དང་མི་རྣམས་ཀྱི་སྟོན་པ། སངས་རྒྱས་བཅོམ་ ལྡན་འདས་དཔལ་རྒྱལ་བ་ཤཱཀྱ་ཐུབ་པ་ལ་ཕྱག་འཚལ་ལོ། མཆོད་དོ་སྐྱབས་ སུ་མཆིའོ། གང་ཚེ་རྐང་གཉིས་གཙོ་བོ་ཁྱོད་བལྟམས་ཚེ། ས་ཆེན་འདི་ ལ་གོམ་པ་བདུན་བོར་ནས། ང་ནི་འཇིག་རྟེན་འདི་ན་མཆོག་ཅེས་གསུངས། དེ་ཚེ་མཁས་པ་ཁྱོད་ལ་ཕྱག་འཚལ་ལོ། རྣམ་དག་སྐུ་མངའ་མཆོག་ཏུ་ གཟུགས་བཟང་བ། ཡེ་ཤེས་རྒྱ་མཚོ་གསེར་གྱི་ལྷུན་པོ་འདྲ། གྲགས་ པ་འཇིག་རྟེན་གསུམ་ན་ལྷམ་མེ་བ། མགོན་པོ་མཆོག་བརྙེས་ཁྱོད་ལ་ཕྱག་ འཚལ་ལོ། མཚན་མཆོག་ལྡན་པ་དྲི་མེད་ཟླ་བའི་ཞལ། གསེར་མདོག་ འདྲ་བ་ཁྱོད་ལ་ཕྱག་འཚལ་ལོ། རྡུལ་བྲལ་ཁྱོད་འདྲ་སྲིད་པ་གསུམ་མ་ མཆིས། མཉམ་མེད་མཁྱེན་ཅན་ཁྱོད་ལ་ཕྱག་འཚལ་ལོ། མགོན་པོ་ཐུགས་ རྗེ་ཆེ་ལྡན་པ། ཐམས་ཅད་མཁྱེན་པས་སྟོན་པ་པོ། བསོད་ནམས་ཡོན་ ཏན་རྒྱ་མཚོའི་ཞིང་། དེ་བཞིན་གཤེགས་ལ་ཕྱག་འཚལ་ལོ། དག་པས་འདོད་ཆགས་བྲལ་བར་གྱུར། དགེ་བས་ངན་སོང་ལས་གྲོལ་ཞིང་། གཅིག་ ཏུ་དོན་དམ་མཆོག་གྱུར་པ། ཞི་གྱུར་ཆོས་ལ་ཕྱག་འཚལ་ལོ། གྲོལ་ནས་ གྲོལ་བའི་ལམ་ཡང་སྟོན། བསླབ་པ་དག་ལ་རབ་དུ་གནས། ཞིང་གི་ དམ་པ་ཡོན་ཏན་ལྡན། དགེ་འདུན་ལ་ཡང་ཕྱག་འཚལ་ལོ། སངས་རྒྱས་ གཙོ་ལ་ཕྱག་འཚལ་ལོ། སྐྱོབ་པ་ཆོས་ལ་ཕྱག་འཚལ་ལོ། དགེ་འདུན་ ཆེ་ལ་ཕྱག་འཚལ་ལོ། གསུམ་ལ་རྟག་ཏུ་གུས་ཕྱག་འཚལ། ཕྱག་བྱར་ འོས་པ་ཐམས་ཅད་ལ། ཞིང་རྡུལ་ཀུན་གྱིས་གྲངས་སྙེད་ཀྱི། ལུས་བཏུད་ པ་ཡིས་རྣམ་ཀུན་ཏུ། མཆོག་ཏུ་དད་པས་ཕྱག་འཚལ་ལོ། སྐར་མ་རབ་ རིབ་མར་མེ་དང་། སྒྱུ་མ་ཟིལ་བ་ཆུ་བུར་དང་། རྨི་ལམ་གློག་དང་སྤྲིན་ ལྟ་བུ། འདུས་བྱས་ཆོས་རྣམས་དེ་ལྟ ར་ལྟ། བསོད་ནམས་འདི་ཡིས་ ཐམས་ཅད་གཟིགས་པ་ཡི། གོ་འཕང་ཐོབ་ནས་སྐྱོན་གྱི་དགྲ་འདུལ་ཏེ། རྒ་དང་ན་དང་འཆི་བའི་རླབས་འཁྲུགས་པའི། སྲིད་པའི་མཚོ་ལས་འགྲོ་བ་\n" +
                        "\n" +
                        "སྒྲོལ་བར་ཤོག །།","url",false,0))
            }
        }
    }
}