import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys

// This will load 'secure_production_profile' profile
CustomKeywords.'SecuredProfile.defineSecuredProfiles'('secured_production_profile') // "./Profiles/secured_production_profile.glbl"
CustomKeywords.'SecuredProfile.loadProfile'('secured_production_profile.glbl') // "./Profiles/secured_production_profile.glbl"
CustomKeywords.'SecuredProfile.loadProfile'('Custom Folder/secured_production_profile') // "./Custom Folder/secured_production_profile.glbl"
CustomKeywords.'SecuredProfile.loadProfile'('C:/Users/me/Desktop/secured_production_profile') // "C:/Users/me/Desktop/secured_production_profile.glbl"

WebUI.openBrowser('')

WebUI.navigateToUrl('https://katalon-demo-cura.herokuapp.com/')

WebUI.click(findTestObject('Object Repository/Page_CURA Healthcare Service/a_Make Appointment'))

WebUI.setText(findTestObject('Object Repository/Page_CURA Healthcare Service/input_username'), GlobalVariable.username)

WebUI.comment('The password here is secured now.');
WebUI.setText(findTestObject('Object Repository/Page_CURA Healthcare Service/input_password'), GlobalVariable.password)

WebUI.click(findTestObject('Object Repository/Page_CURA Healthcare Service/button_Login'))

WebUI.delay(3)

WebUI.closeBrowser()

