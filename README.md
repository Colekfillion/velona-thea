# velona-thea
A lightweight gallery app that is a frontend for a local image metadatabase. <br>
<b> What does that mean? </b> <br>
Data about your images are put into a local SQLite database.. file name, author, tags... which you can search and find images easier. 

Velona Thea does not offer any automatic tagging or such functionality. But you can search using that data if you have it, or create your own media indexing tool that outputs in a txt format.

The format for import/export files are as follows, seperated by tabs:<br>
/folder/folder/filename.png&emsp;name&emsp;author&emsp;your-link-here.com&emsp;tag1 tag2 tag3<br>
/folder/folder/filename.png&emsp;name&emsp;author&emsp;your-link-here.com&emsp;<br>
/folder/folder/filename.png&emsp;name&emsp;author&emsp;&emsp;tag1 tag2 tag3<br>
Tags are seperated by spaces. The end of each line does NOT have a tab, unless you are leaving out
the tags. If you do not include the tag or link, leave that spot blank, but you still have to keep
its tab. So if you leave out a link, there needs to be two tabs before the tags. The end of the
document should have a newline character. When in doubt, import some files using raw import and then
select Export to File in the config.
[example_file.txt](https://github.com/Colekfillion/velona-thea/files/9015261/example_file.txt)

<b> Known issues </b><br>
All versions:<br>
After choosing a directory for raw import, the textbox will not update. To work around this, close the window (back button or tap outside of it) and reopen it.

Gifs have some kind of visual artifacts when they play. It almost looks like a TV that has a damaged screen, or something. This will probably be fixed when I switch to BigImageViewer and start using those image viewing libraries such as Glide, Picasso, etc.

Android 30+:<br>
Files on the SD card cannot be accessed normally - leaving the scope of the system's storage will no longer show files, so you cannot navigate to the SD card. However, if you know the SD card's absolute path, you can type it into the textbox and it should work fine.

The application does not currently work in Samsung's Secure Folder - it constantly asks for Manage
All Files permission, which takes you out of the Secure Folder.

TODO: Convert hardcoded Strings into translatable resources

<b> Screenshots </b><br>
![Screenshot_20220629_185154](https://user-images.githubusercontent.com/84115711/176564396-a45edbfa-ac32-473b-b069-ae002d5e294d.png)
![Screenshot_20220629_185230](https://user-images.githubusercontent.com/84115711/176564489-32e3b39f-7e2c-40ba-afa0-c8ed839d00a8.png)
![Screenshot_20220629_185551](https://user-images.githubusercontent.com/84115711/176564624-11dc0bcf-65b3-4f97-a2b4-19013fd1a861.png)
![Screenshot_20220629_185251](https://user-images.githubusercontent.com/84115711/176564691-93da74cd-11a2-437b-8dee-c9efa4ccb3cd.png)
