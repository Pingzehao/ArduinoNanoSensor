package edu.ucr.nanosensorgrapher;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;


/**
 * FileSelectionPanel is a JPanel that provides a button and label for selecting files. It
 * also has a checkbox, to support the opening of a sequence of files.
 * 
 * @author Albert
 *
 */
public class FileSelectionPanel extends JPanel implements ActionListener
{
	private static final String BUTTON_LABEL = "Open File";
	private static final String CHECKBOX_LABEL = "Load file sequence (files must be in " +
			"the same folder named \"file-01.ext\", \"file-02.ext\", ...)";
	private static final String LABEL_PREFIX = "File: ";
	
	private static final String ERROR_INVALID_SEQUENTIAL_NAME = 
			"Error: Invalid file name for sequential selection";

	private static final int MAX_FILE_NUMBER = 48;

	private JButton mButton;
	private JLabel mFileLabel;
	private JCheckBox mSequenceCheckBox;
	
	private File mSelectedFile;

	public FileSelectionPanel() {
		super(new GridLayout(3, 1));
		super.setBorder(new EmptyBorder(10, 10, 10, 10));
		mButton = new JButton(BUTTON_LABEL);
		mButton.addActionListener(this);
		mFileLabel = new JLabel(LABEL_PREFIX);
		mFileLabel.setHorizontalAlignment(SwingConstants.CENTER);
		mSequenceCheckBox = new JCheckBox(CHECKBOX_LABEL);
		mSequenceCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		super.add(mButton);
		super.add(mFileLabel);
		super.add(mSequenceCheckBox);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == mButton) {
			JFileChooser fileChooser = new JFileChooser();
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				mSelectedFile = fileChooser.getSelectedFile();
				mFileLabel.setText(LABEL_PREFIX + mSelectedFile.getAbsolutePath());
			}
		}
	}
	
	/**
	 * Returns the currently selected file. If no file is selected, null is returned. If
	 * the sequential file is checked return the list of files.
	 * 
	 * @return The currently selected file.
	 */
	public ArrayList<File> getSelectedFiles() {
		if (mSelectedFile == null) {
			return null;
		}

		ArrayList<File> selectedFiles = new ArrayList<File>();
		selectedFiles.add(mSelectedFile);
		if (mSequenceCheckBox.isSelected()) {
			String filePath = mSelectedFile.getPath();
			if (!filePath.matches(".*-\\d+\\..*?")) {
				JOptionPane.showMessageDialog(this, "Error: Invalid sequential file name");
				mSequenceCheckBox.setSelected(false);
				return selectedFiles;
			}
			String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
			String fileNameTruncated = fileName.substring(0, fileName.lastIndexOf("-"));
			String fileExtension = fileName.substring(fileName.lastIndexOf("."));
			String folderPath = 
					filePath.substring(0, filePath.length() - fileName.length());
			String fileNumberString = fileName.substring(
					fileName.lastIndexOf("-") + 1, fileName.lastIndexOf("."));
			/** +1 to get the next file number **/
			int fileNumber = Integer.parseInt(fileNumberString) + 1;
			
			File nextFile;
			for (int i = fileNumber; i < MAX_FILE_NUMBER; ++i) {
				String nextFilePath = folderPath + fileNameTruncated + "-" + i + 
						fileExtension;
				nextFile = new File(nextFilePath);
				if (!nextFile.canRead()) {
					System.err.println(nextFilePath + " not found.");
				} else {
					selectedFiles.add(nextFile);
				}
			}
		}
		return selectedFiles;
	}
	
	/**
	 * Returns whether or not the checkbox is checked. Used for determining whether or
	 * not to process a sequence of files.
	 * @return Whether or not the checkbox is checked.
	 */
	public boolean isSequence() {
		return mSequenceCheckBox.isSelected();
	}
}
