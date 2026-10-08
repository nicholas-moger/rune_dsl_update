package chaos.s25.a4none;

import chaos.s25.a4none.meta.C25OptBMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Choice option B.
 * @version 0.0.0
 */
@RosettaDataType(value="C25OptB", builder=C25OptB.C25OptBBuilderImpl.class, version="0.0.0")
@RuneDataType(value="C25OptB", model="chaos", builder=C25OptB.C25OptBBuilderImpl.class, version="0.0.0")
public interface C25OptB extends RosettaModelObject {

	C25OptBMeta metaData = new C25OptBMeta();

	/*********************** Getter Methods  ***********************/
	String getPb();
	String getShared();

	/*********************** Build Methods  ***********************/
	C25OptB build();
	
	C25OptB.C25OptBBuilder toBuilder();
	
	static C25OptB.C25OptBBuilder builder() {
		return new C25OptB.C25OptBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C25OptB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C25OptB> getType() {
		return C25OptB.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("pb"), String.class, getPb(), this);
		processor.processBasic(path.newSubPath("shared"), String.class, getShared(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C25OptBBuilder extends C25OptB, RosettaModelObjectBuilder {
		C25OptB.C25OptBBuilder setPb(String pb);
		C25OptB.C25OptBBuilder setShared(String shared);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("pb"), String.class, getPb(), this);
			processor.processBasic(path.newSubPath("shared"), String.class, getShared(), this);
		}
		

		C25OptB.C25OptBBuilder prune();
	}

	/*********************** Immutable Implementation of C25OptB  ***********************/
	class C25OptBImpl implements C25OptB {
		private final String pb;
		private final String shared;
		
		protected C25OptBImpl(C25OptB.C25OptBBuilder builder) {
			this.pb = builder.getPb();
			this.shared = builder.getShared();
		}
		
		@Override
		@RosettaAttribute("pb")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pb")
		public String getPb() {
			return pb;
		}
		
		@Override
		@RosettaAttribute("shared")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("shared")
		public String getShared() {
			return shared;
		}
		
		@Override
		public C25OptB build() {
			return this;
		}
		
		@Override
		public C25OptB.C25OptBBuilder toBuilder() {
			C25OptB.C25OptBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C25OptB.C25OptBBuilder builder) {
			ofNullable(getPb()).ifPresent(builder::setPb);
			ofNullable(getShared()).ifPresent(builder::setShared);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25OptB _that = getType().cast(o);
		
			if (!Objects.equals(pb, _that.getPb())) return false;
			if (!Objects.equals(shared, _that.getShared())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pb != null ? pb.hashCode() : 0);
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25OptB {" +
				"pb=" + this.pb + ", " +
				"shared=" + this.shared +
			'}';
		}
	}

	/*********************** Builder Implementation of C25OptB  ***********************/
	class C25OptBBuilderImpl implements C25OptB.C25OptBBuilder {
	
		protected String pb;
		protected String shared;
		
		@Override
		@RosettaAttribute("pb")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pb")
		public String getPb() {
			return pb;
		}
		
		@Override
		@RosettaAttribute("shared")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("shared")
		public String getShared() {
			return shared;
		}
		
		@RosettaAttribute("pb")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pb")
		@Override
		public C25OptB.C25OptBBuilder setPb(String _pb) {
			this.pb = _pb == null ? null : _pb;
			return this;
		}
		
		@RosettaAttribute("shared")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("shared")
		@Override
		public C25OptB.C25OptBBuilder setShared(String _shared) {
			this.shared = _shared == null ? null : _shared;
			return this;
		}
		
		@Override
		public C25OptB build() {
			return new C25OptB.C25OptBImpl(this);
		}
		
		@Override
		public C25OptB.C25OptBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25OptB.C25OptBBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPb()!=null) return true;
			if (getShared()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25OptB.C25OptBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C25OptB.C25OptBBuilder o = (C25OptB.C25OptBBuilder) other;
			
			
			merger.mergeBasic(getPb(), o.getPb(), this::setPb);
			merger.mergeBasic(getShared(), o.getShared(), this::setShared);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25OptB _that = getType().cast(o);
		
			if (!Objects.equals(pb, _that.getPb())) return false;
			if (!Objects.equals(shared, _that.getShared())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pb != null ? pb.hashCode() : 0);
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25OptBBuilder {" +
				"pb=" + this.pb + ", " +
				"shared=" + this.shared +
			'}';
		}
	}
}
