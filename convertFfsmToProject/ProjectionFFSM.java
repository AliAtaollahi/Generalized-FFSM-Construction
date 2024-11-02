package uk.le.ac.convertFfsmToProject;

public class ProjectionFFSM {
	public static void main(String[] args) {
		FFSMConvertor ffsm_convertor = new FFSMConvertor();
		ffsm_convertor.start_loading_ffsm_and_convert_to_fsm(args);
	}
}
